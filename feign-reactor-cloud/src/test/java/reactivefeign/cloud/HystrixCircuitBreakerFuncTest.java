package reactivefeign.cloud;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.RequestLine;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {HystrixCircuitBreakerFuncTest.class, HystrixCircuitBreakerFuncTest.TestConfig.class},
        properties = {
                "wiremock.port=${wiremock.server.port}",
                "hystrix.command.default.circuitBreaker.requestVolumeThreshold=3",
                "hystrix.command.default.circuitBreaker.enabled=false"
        })
@AutoConfigureWireMock(port = 8899)
@EnableAutoConfiguration
public class HystrixCircuitBreakerFuncTest {
    private static final String TEST_URL = "/call";
    private static final String FALLBACK = "fallback";
    private static final String CIRCUIT_IS_OPEN = "short-circuited";
    private static final String NO_FALLBACK = "and no fallback available";

    @Value("${wiremock.port}")
    private int WIREMOCK_PORT;
    @Value("${hystrix.command.default.circuitBreaker.requestVolumeThreshold}")
    private int HYSTRIX_VOLUME_THRESHOLD;

    @After
    public void tearDown() {
        reset();
    }


    @Test
    public void shouldReturnFallbackWithClosedCircuitAfterThreshold() {
        int callsNo = HYSTRIX_VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = CloudReactiveFeign.<TestCaller>builder()
                .setFallback(() -> Mono.just(FALLBACK))
                .target(TestCaller.class, "http://localhost:" + WIREMOCK_PORT);

        //check that circuit breaker DOESN'T open on volume threshold
        List<Object> results = IntStream.range(0, callsNo)
                .mapToObj(i -> testCaller.call().block())
                .collect(Collectors.toList());

        // check fallback invokes each time
        assertThat(results).containsOnly(FALLBACK);

        // assert circuit wasn't open, so all requests went to server
        verify(exactly(callsNo), getRequestedFor(urlEqualTo(TEST_URL)));
    }

    @Test
    public void shouldNotOpenCircuitAfterThreshold() {
        int callsNo = HYSTRIX_VOLUME_THRESHOLD + 10;
        mockResponseServiceUnavailable();

        TestCaller testCaller = CloudReactiveFeign.<TestCaller>builder()
                .target(TestCaller.class, "http://localhost:" + WIREMOCK_PORT);

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

    interface TestCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<String> call();
    }

    @Configuration
    static class TestConfig {
        @Bean
        public ReactiveWebServerFactory reactiveWebServerFactory() {
            return new NettyReactiveWebServerFactory();
        }
    }
}
