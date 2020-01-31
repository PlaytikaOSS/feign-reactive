package reactivefeign.cloud.common;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud.LoadBalancingReactiveHttpClientTest.TestMonoInterface;

/**
 * @author Sergii Karpenko
 */
abstract public class AbstractCircuitBreakerReactiveHttpClientTest {

    public static final int SLEEP_WINDOW = 1000;
    public static final int VOLUME_THRESHOLD = 4;
    public static final String FALLBACK = "fallback";
    public static final String SUCCESS = "success!";
    public static final int UPDATE_INTERVAL = 5;

    @Rule
    public WireMockRule server = new WireMockRule(wireMockConfig().dynamicPort());

    protected AtomicReference<String> lastCommandKey = new AtomicReference<>();

    @Before
    public void resetServers() {
        server.resetAll();
    }

    @Test
    public void shouldFailAsNoFallback() {

        String body = "success!";
        AbstractLoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, AbstractLoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono())
                .expectErrorMatches(this::assertNoFallback)
                .verify();
    }

    @Test
    public void shouldNotFailDueToFallback() {

        String body = "success!";
        AbstractLoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, AbstractLoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
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
        AbstractLoadBalancingReactiveHttpClientTest.mockSuccessAfterSeveralAttempts(server, AbstractLoadBalancingReactiveHttpClientTest.MONO_URL, 1, 598,
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body));

        TestMonoInterface client = cloudBuilderWithTimeoutDisabled()
                .fallbackFactory(throwable -> () -> {throw new RuntimeException();})
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono())
                .expectErrorMatches(this::assertFailedAndFallbackFailed)
                .verify();
    }

    @Test
    public void shouldOpenCircuitBreakerAndCloseAfterSleepTime() throws InterruptedException {

        assertThat(server.getAllServeEvents()).isEmpty();

        int callsNo = VOLUME_THRESHOLD + 1;

        server.stubFor(get(urlEqualTo(AbstractLoadBalancingReactiveHttpClientTest.MONO_URL))
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
        assertCircuitBreakerAllowRequests(throwableCircuitClosed);

        //wait to get opened
        waitCircuitBreakerToGetOpened();

        Throwable throwableCircuitOpened = client.getMono()
                .subscribeOn(Schedulers.parallel())
                .<Throwable>map(s -> null)
                .onErrorResume(t -> true, Mono::just).block();
        assertCircuitBreakerOpen(throwableCircuitOpened);
        assertThat(server.getAllServeEvents().size()).isLessThan(callsNo);

        //wait to circuit breaker get closed again
        waitCircuitBreakerToAllowRequestAgain();

        server.stubFor(get(urlEqualTo(AbstractLoadBalancingReactiveHttpClientTest.MONO_URL))
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

        server.stubFor(get(urlEqualTo(AbstractLoadBalancingReactiveHttpClientTest.MONO_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("success!")));

        TestMonoInterface client = cloudBuilderWithTimeout(500)
                .target(TestMonoInterface.class, "http://localhost:" + server.port());

        StepVerifier.create(client.getMono().subscribeOn(Schedulers.parallel()))
                .expectErrorMatches(this::assertTimeout)
                .verify();
    }

    abstract protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeoutDisabled();

    abstract protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeout(int timeoutMs);

    abstract protected boolean assertNoFallback(Throwable throwable);

    abstract protected void assertCircuitBreakerOpen(Throwable throwableCircuitOpened);

    abstract protected boolean assertFailedAndFallbackFailed(Throwable throwable);

    private void waitCircuitBreakerToGetOpened() {
        Awaitility.waitAtMost(Duration.ofSeconds(1))
                .pollDelay(Duration.ofMillis(UPDATE_INTERVAL))
                .untilAsserted(this::assertCircuitBreakerOpen);
    }

    protected void waitCircuitBreakerToAllowRequestAgain() {
        try {
            Thread.sleep(SLEEP_WINDOW);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    abstract protected void assertCircuitBreakerAllowRequests(Throwable throwable);

    abstract protected void assertCircuitBreakerAllowRequests();

    abstract protected void assertCircuitBreakerClosed();

    abstract protected void assertCircuitBreakerOpen();

    abstract protected boolean assertTimeout(Throwable throwable);

}
