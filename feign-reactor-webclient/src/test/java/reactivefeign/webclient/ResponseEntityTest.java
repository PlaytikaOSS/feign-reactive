package reactivefeign.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RequestLine;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Flavor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static reactivefeign.TestUtils.MAPPER;
import static reactivefeign.TestUtils.toLowerCaseKeys;
import static reactivefeign.webclient.WebReactiveFeign.builder;

public class ResponseEntityTest {

    public static final String TEST_URL = "call";
    @Rule
    public WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig().dynamicPort());

    @Test
    public void shouldPassResponseAsResponseEntity() {

        wireMockRule.stubFor(get(urlEqualTo("/" + TEST_URL))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[1, 2]")));

        TestCaller client = WebReactiveFeign.<TestCaller>builder()
                .target(TestCaller.class, "http://localhost:" + wireMockRule.port());

        Mono<ResponseEntity<Flux<Integer>>> result = client.call();
        StepVerifier.create(result)
                .expectNextMatches(response -> toLowerCaseKeys(response.getHeaders())
                        .containsKey("content-type"))
                .verifyComplete();

        StepVerifier.create(result.flatMapMany(HttpEntity::getBody))
                .expectNextSequence(asList(1, 2))
                .verifyComplete();
    }

    public interface TestCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<ResponseEntity<Flux<Integer>>> call();
    }
}
