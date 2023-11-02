package reactivefeign.cloud2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import reactivefeign.BaseReactorTest;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud2.LoadBalancingReactiveHttpClientTest.*;

/**
 * @author Sergii Karpenko
 */
public class CircuitBreakerReactiveHttpClientTest extends BaseReactorTest {

    public static final int SLEEP_WINDOW = 1000;
    public static final int VOLUME_THRESHOLD = 4;
    public static final String FALLBACK = "fallback";
    public static final String SUCCESS = "success!";
    public static final int UPDATE_INTERVAL = 5;

    @Rule
    public WireMockRule server = new WireMockRule(wireMockConfig().dynamicPort());

    protected AtomicReference<String> lastCommandKey = new AtomicReference<>();

    private static CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private static ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory
            = new ReactiveResilience4JCircuitBreakerFactory(
        CircuitBreakerRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults()
    );

    @BeforeClass
    public static void setupServersList() {
        circuitBreakerFactory.configureCircuitBreakerRegistry(circuitBreakerRegistry);
    }

    @Before
    public void resetServers() {
        server.resetAll();
    }

    @Test
    public void shouldFailAsNoFallback() {

        String body = "success!";
        mockSuccessAfterSeveralAttempts(server, MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono().subscribeOn(testScheduler()))
                .expectErrorMatches(this::assertNoFallback)
                .verify();
    }

    @Test
    public void shouldNotFailDueToFallback() {

        String body = "success!";
        mockSuccessAfterSeveralAttempts(server, MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .fallback(() -> Mono.just(FALLBACK))
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        String result = client.getMono().subscribeOn(testScheduler()).block();
        assertThat(result).isEqualTo(FALLBACK);
    }

    @Test
    public void shouldFailDueToErrorInFallback() {

        String body = "success!";
        mockSuccessAfterSeveralAttempts(server, MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .fallbackFactory(throwable -> () -> {throw new RuntimeException();})
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono().subscribeOn(testScheduler()))
                .expectErrorMatches(this::assertFailedAndFallbackFailed)
                .verify();
    }

    @Test
    public void shouldOpenCircuitBreakerAndCloseAfterSleepTime() {

        assertThat(server.getAllServeEvents()).isEmpty();

        int callsNo = VOLUME_THRESHOLD + 1;

        server.stubFor(get(urlEqualTo(MONO_URL))
                .willReturn(aResponse()
                        .withStatus(SC_SERVICE_UNAVAILABLE)
                        .withHeader("Retry-After", "1")));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        //check that circuit breaker get opened on volume threshold
        List<Throwable> throwablesCircuitClosed = Flux.range(0, VOLUME_THRESHOLD)
                .concatMap(i -> client.getMono()
                        .subscribeOn(testScheduler())
                        .<Throwable>map(s -> null)
                        .onErrorResume(t -> true, Mono::just))
                .collectList().block();

        Throwable throwableCircuitClosed = throwablesCircuitClosed.get(0);
        assertCircuitBreakerAllowRequests(throwableCircuitClosed);

        //wait to get opened
        waitCircuitBreakerToGetOpened();

        Throwable throwableCircuitOpened = client.getMono()
                .subscribeOn(testScheduler())
                .<Throwable>map(s -> null)
                .onErrorResume(t -> true, Mono::just).block();
        assertCircuitBreakerOpen(throwableCircuitOpened);
        assertThat(server.getAllServeEvents().size()).isLessThan(callsNo);

        //wait to circuit breaker get closed again
        waitCircuitBreakerToAllowRequestAgain();

        server.stubFor(get(urlEqualTo(MONO_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESS)));

        //check that circuit breaker get closed again
        List<Object> resultsAfterSleep = IntStream.range(0, callsNo).mapToObj(i -> {
            try {
                return client.getMono().block();
            } catch (Throwable t) {
                return t;
            }
        }).collect(Collectors.toList());

        assertThat(resultsAfterSleep).containsOnly(SUCCESS);
        assertCircuitBreakerClosed();
    }

    @Test
    public void shouldFailOnTimeoutExceptionOnDefaultSettings() {

        server.stubFor(get(urlEqualTo(MONO_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("success!")));

        TestMonoInterface client = cloudBuilderWithTimeout(500)
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono().subscribeOn(testScheduler()))
                .expectErrorMatches(this::assertTimeout)
                .verify();
    }

    private void waitCircuitBreakerToGetOpened() {
        Awaitility.waitAtMost(Duration.ofSeconds(1))
                .pollDelay(Duration.ofMillis(UPDATE_INTERVAL))
                .untilAsserted(this::assertCircuitBreakerOpen);
    }

    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeoutDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .minimumNumberOfCalls(VOLUME_THRESHOLD)
                                .enableAutomaticTransitionFromOpenToHalfOpen()
                                .waitDurationInOpenState(Duration.ofMillis(SLEEP_WINDOW)).build())
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMinutes(1)).build()),
                lastCommandKey);
    }

    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeout(int timeoutMs){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(timeoutMs)).build()),
                lastCommandKey);
    }

    protected void assertCircuitBreakerOpen(Throwable throwableCircuitOpened) {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    protected boolean assertNoFallback(Throwable throwable) {
        return throwable instanceof NoFallbackAvailableException;
    }

    protected boolean assertFailedAndFallbackFailed(Throwable throwable) {
        return throwable instanceof RuntimeException;
    }

    protected void assertCircuitBreakerAllowRequests(Throwable throwable) {
        assertThat(throwable).isInstanceOf(NoFallbackAvailableException.class);
        assertThat(throwable.getCause()).isInstanceOf(RetryableException.class);
    }

    protected void assertCircuitBreakerAllowRequests() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isIn(CircuitBreaker.State.CLOSED, CircuitBreaker.State.HALF_OPEN);
    }

    protected void assertCircuitBreakerClosed() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isIn(CircuitBreaker.State.CLOSED);
    }

    protected void assertCircuitBreakerOpen() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    protected boolean assertTimeout(Throwable throwable) {
        return throwable instanceof NoFallbackAvailableException
                && throwable.getCause() instanceof TimeoutException;
    }

    protected void waitCircuitBreakerToAllowRequestAgain() {
        Awaitility.waitAtMost(Duration.ofMillis(SLEEP_WINDOW * 2))
                .pollDelay(Duration.ofMillis(UPDATE_INTERVAL))
                .untilAsserted(this::assertCircuitBreakerAllowRequests);
    }


}
