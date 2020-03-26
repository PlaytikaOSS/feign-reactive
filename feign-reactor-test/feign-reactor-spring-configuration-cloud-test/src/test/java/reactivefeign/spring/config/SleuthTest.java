package reactivefeign.spring.config;


import brave.Span;
import brave.Tracer;
import brave.sampler.Sampler;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.util.ArrayListSpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import zipkin2.reporter.Reporter;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.SleuthTest.FEIGN_CLIENT_TEST_SLEUTH;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SleuthTest.TestConfiguration.class, SleuthTest.TestController.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.sleuth.enabled=true",
        FEIGN_CLIENT_TEST_SLEUTH+".ribbon.listOfServers=localhost:${local.server.port}"},
        locations = {"classpath:ribbon-enabled-hystrix-disabled.properties",
                "classpath:common.properties"})
@DirtiesContext
public class SleuthTest {

    static final String TRACE_ID_NAME = "X-B3-TraceId";
    static final String SPAN_ID_NAME = "X-B3-SpanId";
    static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";

    static final String FEIGN_CLIENT_TEST_SLEUTH = "feign-client-test-sleuth";

    @Autowired
    TestFeignInterface feignClient;

    @Autowired
    ArrayListSpanReporter reporter;

    @Autowired
    Tracer tracer;

    @After
    public void reset() {
        this.reporter.clear();
    }

    @Test
    public void shouldKeepOriginalTraceId() {
        Span span = this.tracer.nextSpan().name("foo").start();

        String currentTraceId = span.context().traceIdString();
        String currentSpanId = span.context().spanIdString();

        try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span)) {
            Map<String, String> response = feignClient.headers().block();

            assertThat(response.get(TRACE_ID_NAME)).isEqualTo(currentTraceId);
            assertThat(response.get(PARENT_SPAN_ID_NAME)).isEqualTo(currentSpanId);
            assertThat(response.get(SPAN_ID_NAME)).isNotEqualTo(currentSpanId);
        }
        finally {
            span.finish();
        }

        assertThat(this.tracer.currentSpan()).isNull();
        assertThat(this.reporter.getSpans()).isNotEmpty();
    }

    @ReactiveFeignClient(name = FEIGN_CLIENT_TEST_SLEUTH)
    public interface TestFeignInterface {

        @RequestMapping(method = RequestMethod.GET, value = "/")
        Mono<Map<String, String>> headers();

    }

    @Configuration
    @EnableAutoConfiguration
    @EnableReactiveFeignClients(clients = TestFeignInterface.class)
    public static class TestConfiguration {
        @Bean
        Sampler sampler() {
            return Sampler.ALWAYS_SAMPLE;
        }
        @Bean
        Reporter<zipkin2.Span> spanReporter() {
            return new ArrayListSpanReporter();
        }
    }

    @RestController
    public static class TestController {

        @RequestMapping("/")
        public Map<String, String> headers(@RequestHeader HttpHeaders headers) {
            Map<String, String> map = new HashMap<>();
            for (String key : headers.keySet()) {
                map.put(key, headers.getFirst(key));
            }
            return map;
        }

    }

}