package reactivefeign.cloud;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.RequestLine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {HystrixCircuitBreakerFuncTest.class},
        properties = {
                "hystrix.command.default.circuitBreaker.requestVolumeThreshold=3",
                "hystrix.command.default.circuitBreaker.enabled=false",
                "hystrix.command.default.execution.timeout.enabled=false"
        })
@EnableAutoConfiguration
public class HystrixCircuitBreakerFuncTest {
    private static final String TEST_URL = "/call";
    private static final String FALLBACK = "fallback";
    private static final String CIRCUIT_IS_OPEN = "short-circuited";
    private static final String NO_FALLBACK = "and no fallback available";

    @Rule
    public WireMockClassRule wireMockRule = new WireMockClassRule(
            WireMockConfiguration.wireMockConfig()
                    .dynamicPort());

    @Value("${hystrix.command.default.circuitBreaker.requestVolumeThreshold}")
    private int HYSTRIX_VOLUME_THRESHOLD;

    protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled(){
        return BuilderUtils.cloudBuilder();
    }

    @Test
    public void shouldReturnFallbackWithClosedCircuitAfterThreshold() {
        int callsNo = HYSTRIX_VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled()
                .fallback(() -> Mono.just(FALLBACK))
                .target(TestCaller.class, "http://localhost:" + wireMockRule.port());

        //check that circuit breaker DOESN'T open on volume threshold
        List<Object> results = IntStream.range(0, callsNo)
                .mapToObj(i -> testCaller.call().block())
                .collect(Collectors.toList());

        // check fallback invokes each time
        assertThat(results).containsOnly(FALLBACK);

        // assert circuit wasn't open, so all requests went to server
        wireMockRule.verify(callsNo, getRequestedFor(urlEqualTo(TEST_URL)));
    }

    @Test
    public void shouldNotOpenCircuitAfterThreshold() {
        int callsNo = HYSTRIX_VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled()
                .target(TestCaller.class, "http://localhost:" + wireMockRule.port());

        //check that circuit breaker DOESN'T open on volume threshold
        List<Object> results = IntStream.range(0, callsNo).mapToObj(i -> {
            try {
                return testCaller.call().block();
            } catch (Throwable t) {
                return t;
            }
        }).collect(Collectors.toList());

        // all exceptions before and after volume threshold are the same
        Throwable firstError = (Throwable) results.get(0);
        assertThat(firstError).isInstanceOf(HystrixRuntimeException.class);
        assertThat(firstError.getMessage()).contains(NO_FALLBACK).doesNotContain(CIRCUIT_IS_OPEN);

        Throwable lastError = (Throwable) results.get(results.size() - 1);
        assertThat(lastError).isInstanceOf(HystrixRuntimeException.class);
        assertThat(lastError.getMessage()).contains(NO_FALLBACK).doesNotContain(CIRCUIT_IS_OPEN);

        // assert circuit is still closed, so all requests went to server
        verify(exactly(callsNo), getRequestedFor(urlEqualTo(TEST_URL)));
    }

    private void mockResponseServiceUnavailable() {
        stubFor(get(urlEqualTo(TEST_URL)).willReturn(aResponse().withStatus(503)));
    }

    public interface TestCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<String> call();
    }
}
