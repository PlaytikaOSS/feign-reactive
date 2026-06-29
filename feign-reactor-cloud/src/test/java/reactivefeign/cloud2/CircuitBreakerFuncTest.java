package reactivefeign.cloud2;


import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.RequestLine;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactivefeign.BaseReactorTest;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = reactivefeign.cloud2.CircuitBreakerFuncTest.class)
@EnableAutoConfiguration
public class CircuitBreakerFuncTest extends BaseReactorTest {

    protected static final int VOLUME_THRESHOLD = 3;
    private static final String TEST_URL = "/call";
    private static final String FALLBACK = "fallback";

    @RegisterExtension
    public WireMockExtension wireMockRule = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig()
            .dynamicPort()).build();

    private static ReactiveCircuitBreakerFactory circuitBreakerFactory;

    @BeforeAll
    public static void setupCircuitBreakerFactory() {
        circuitBreakerFactory = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null, new org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties());
    }

    @Test
    public void shouldReturnFallbackWithClosedCircuitAfterThreshold() {
        int callsNo = VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled()
                .fallback(() -> Mono.just(FALLBACK))
                .target(TestCaller.class, "http://localhost:" + wireMockRule.getPort());

        //check that circuit breaker DOESN'T open on volume threshold
        List<Object> results = IntStream.range(0, callsNo)
                .mapToObj(i -> testCaller.call().subscribeOn(testScheduler()).block())
                .collect(Collectors.toList());

        // check fallback invokes each time
        assertThat(results).containsOnly(FALLBACK);

        // assert circuit wasn't open, so all requests went to server
        wireMockRule.verify(callsNo, getRequestedFor(urlEqualTo(TEST_URL)));
    }

    @Test
    public void shouldNotOpenCircuitAfterThreshold() {
        int callsNo = VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled()
                .target(TestCaller.class, "http://localhost:" + wireMockRule.getPort());

        //check that circuit breaker DOESN'T open on volume threshold
        List<Object> results = IntStream.range(0, callsNo).mapToObj(i -> {
            try {
                return testCaller.call().subscribeOn(testScheduler()).block();
            } catch (Throwable t) {
                return t;
            }
        }).collect(Collectors.toList());

        // all exceptions before and after volume threshold are the same
        Throwable firstError = (Throwable) results.get(0);
        assertCircuitBreakerClosed(firstError);

        Throwable lastError = (Throwable) results.get(results.size() - 1);
        assertCircuitBreakerClosed(lastError);

        // assert circuit is still closed, so all requests went to server
        wireMockRule.verify(exactly(callsNo), getRequestedFor(urlEqualTo(TEST_URL)));
    }

    private void mockResponseServiceUnavailable() {
        wireMockRule.stubFor(get(urlEqualTo(TEST_URL)).willReturn(aResponse().withStatus(503)));
    }

    public interface TestCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<String> call();
    }

    protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMinutes(1)).build())
                        .circuitBreakerConfig(CircuitBreakerConfig.custom().minimumNumberOfCalls(Integer.MAX_VALUE).build()),
                null);
    }

    protected void assertCircuitBreakerClosed(Throwable throwable) {
        assertThat(throwable).isInstanceOf(NoFallbackAvailableException.class);
    }

}
