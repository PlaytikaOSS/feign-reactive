package reactivefeign.webclient;

import feign.RequestLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {"spring.main.web-application-type=reactive"},
        classes = {ClientCustomizerTest.TestController.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class ClientCustomizerTest {

    @LocalServerPort
    private int port;

    private TestApi client;

    @Before
    public void setUp() {
        client = WebReactiveFeign.<TestApi>builder()
                .addCustomizer(httpClient -> httpClient.headers(h -> h.add("X-Custom", "value")))
                .target(TestApi.class, "http://localhost:" + port);
    }

    @Test
    public void shouldCustomizeClient() {
        StepVerifier.create(client.test())
                .expectNext("value")
                .verifyComplete();
    }

    @RestController
    public static class TestController {
        @GetMapping("/test")
        public Mono<String> test(@RequestHeader("X-Custom") String customHeader) {
            return Mono.just(customHeader);
        }
    }

    interface TestApi {
        @RequestLine("GET /test")
        Mono<String> test();
    }
}
