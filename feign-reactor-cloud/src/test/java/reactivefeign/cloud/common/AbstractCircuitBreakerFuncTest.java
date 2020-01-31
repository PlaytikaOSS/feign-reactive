package reactivefeign.cloud.common;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.RequestLine;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

abstract public class AbstractCircuitBreakerFuncTest {

    protected static final int VOLUME_THRESHOLD = 3;
    private static final String TEST_URL = "/call";
    private static final String FALLBACK = "fallback";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.wireMockConfig()
                    .dynamicPort());

    abstract protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled();

    abstract protected void assertCircuitBreakerClosed(Throwable throwable);

    @Test
    public void shouldReturnFallbackWithClosedCircuitAfterThreshold() {
        int callsNo = VOLUME_THRESHOLD + 10;
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
        int callsNo = VOLUME_THRESHOLD + 10;
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
        assertCircuitBreakerClosed(firstError);

        Throwable lastError = (Throwable) results.get(results.size() - 1);
        assertCircuitBreakerClosed(lastError);

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
