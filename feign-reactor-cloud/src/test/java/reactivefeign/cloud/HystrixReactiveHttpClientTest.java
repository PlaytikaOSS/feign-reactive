package reactivefeign.cloud;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static reactivefeign.cloud.LoadBalancingReactiveHttpClientTest.TestMonoInterface;

/**
 * @author Sergii Karpenko
 */
public class HystrixReactiveHttpClientTest {

    public static final int SLEEP_WINDOW = 1000;
    public static final int VOLUME_THRESHOLD = 4;
    public static final String FALLBACK = "fallback";
    public static final String SUCCESS = "success!";
    public static final int UPDATE_INTERVAL = 5;

    @Rule
    public WireMockRule server = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected AtomicReference<HystrixCommandKey> lastCommandKey = new AtomicReference<>();

    @Before
    public void resetServers() {
        server.resetAll();
    }

    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeoutDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueHystrixCommand(
                hystrixPropertiesTimeoutDisabled(), lastCommandKey);
    }

    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeout(int timeoutMs){
        return BuilderUtils.<TestMonoInterface>cloudBuilderWithUniqueHystrixCommand(
                hystrixProperties()
                        .withExecutionTimeoutEnabled(true)
                        .withExecutionTimeoutInMilliseconds(timeoutMs), lastCommandKey);
    }

    @Test
    public void shouldFailAsNoFallback() {

        expectedException.expect(HystrixRuntimeException.class);
        expectedException.expectMessage(containsString("failed and no fallback available"));

        String body = "success!";
        LoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, LoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        client.getMono().block();
    }

    @Test
    public void shouldNotFailDueToFallback() {

        String body = "success!";
        LoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, LoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .fallback(() -> Mono.just(FALLBACK))
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        String result = client.getMono().block();
        assertThat(result).isEqualTo(FALLBACK);
    }

    @Test
    public void shouldFailDueToErrorInFallback() {

        String body = "success!";
        LoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, LoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .fallbackFactory(throwable -> () -> {throw new RuntimeException();})
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono())
                .expectErrorMatches(throwable -> throwable instanceof HystrixRuntimeException
                        && throwable.getMessage().contains("failed and fallback failed"))
                .verify();
    }

    @Test
    public void shouldOpenCircuitBreakerAndCloseAfterSleepTime() throws InterruptedException {

        assertThat(server.getAllServeEvents()).isEmpty();

        int callsNo = VOLUME_THRESHOLD + 1;

        server.stubFor(get(urlEqualTo(LoadBalancingReactiveHttpClientTest.MONO_URL))
                .willReturn(aResponse()
                        .withStatus(SC_SERVICE_UNAVAILABLE)
                        .withHeader("Retry-After", "1")));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        //check that circuit breaker get opened on volume threshold
        List<Throwable> throwablesCircuitClosed = Flux.range(0, VOLUME_THRESHOLD)
                .concatMap(i -> client.getMono()
                        .subscribeOn(Schedulers.parallel())
                        .<Throwable>map(s -> null)
                        .onErrorResume(t -> true, Mono::just))
                .collectList().block();

        Throwable throwableCircuitClosed = throwablesCircuitClosed.get(0);
        assertThat(throwableCircuitClosed).isInstanceOf(HystrixRuntimeException.class);
        assertThat(throwableCircuitClosed.getMessage())
                .contains("and no fallback available")
                .doesNotContain("short-circuited");


        //wait to get opened
        Awaitility.waitAtMost(Duration.ONE_SECOND)
                .pollDelay(new Duration(UPDATE_INTERVAL, TimeUnit.MILLISECONDS))
                .until(() -> HystrixCircuitBreaker.Factory.getInstance(lastCommandKey.get()).isOpen());

        Throwable throwableCircuitOpened = client.getMono()
                .subscribeOn(Schedulers.parallel())
                .<Throwable>map(s -> null)
                .onErrorResume(t -> true, Mono::just).block();

        assertThat(throwableCircuitOpened).isInstanceOf(HystrixRuntimeException.class);
        assertThat(throwableCircuitOpened.getMessage())
                .contains("short-circuited and no fallback available.");
        assertThat(server.getAllServeEvents().size()).isLessThan(callsNo);

        //wait to circuit breaker get closed again
        Thread.sleep(SLEEP_WINDOW);

        server.stubFor(get(urlEqualTo(LoadBalancingReactiveHttpClientTest.MONO_URL))
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
        assertThat(HystrixCircuitBreaker.Factory.getInstance(lastCommandKey.get())
                .isOpen())
                .isFalse();
    }

    @Test
    public void shouldFailOnTimeoutExceptionOnDefaultSettings() {

        server.stubFor(get(urlEqualTo(LoadBalancingReactiveHttpClientTest.MONO_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("success!")));

        TestMonoInterface client = cloudBuilderWithTimeout(500)
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono().subscribeOn(Schedulers.parallel()))
                .expectErrorMatches(throwable -> throwable instanceof HystrixRuntimeException
                        && throwable.getCause() instanceof TimeoutException)
                .verify();
    }

    protected static HystrixCommandProperties.Setter hystrixProperties(){
        return HystrixCommandProperties.Setter()
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerRequestVolumeThreshold(VOLUME_THRESHOLD)
                .withCircuitBreakerSleepWindowInMilliseconds(SLEEP_WINDOW)
                .withMetricsRollingStatisticalWindowInMilliseconds(1000)
                .withMetricsRollingStatisticalWindowBuckets(1)
                .withMetricsHealthSnapshotIntervalInMilliseconds(UPDATE_INTERVAL);
    }

    protected static HystrixCommandProperties.Setter hystrixPropertiesTimeoutDisabled(){
        return hystrixProperties()
                .withExecutionTimeoutEnabled(false);
    }

}
