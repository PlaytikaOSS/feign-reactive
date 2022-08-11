package reactivefeign.webclient.core;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RequestLine;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

abstract public class ResponseEntityTest {

    public static final String TEST_URL = "call";
    @Rule
    public WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    abstract protected <T> ReactiveFeignBuilder<T> builder();

    @Test
    public void shouldPassResponseAsResponseEntity() {

        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + TEST_URL))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[1, 2]")));

        TestCaller client = this.<TestCaller>builder()
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

    @Test
    public void shouldPassResponseAsRawResponseEntity() {

        wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/" + TEST_URL))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[1, 2]")));

        TestCaller client = this.<TestCaller>builder()
                .target(TestCaller.class, "http://localhost:" + wireMockRule.port());

        Mono<ResponseEntity<Mono<byte[]>>> resultRaw = client.callRaw();

        StepVerifier.create(resultRaw)
                .expectNextMatches(response -> toLowerCaseKeys(response.getHeaders())
                        .containsKey("content-type"))
                .verifyComplete();

        StepVerifier.create(resultRaw.flatMapMany(HttpEntity::getBody))
                .expectNextMatches(bytes -> Arrays.equals("[1, 2]".getBytes(), bytes))
                .verifyComplete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNonReactiveParameterInResponseEntity() {
        this.<WrongCaller>builder()
                .target(WrongCaller.class, "http://localhost:" + wireMockRule.port());
    }

    public interface TestCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<ResponseEntity<Flux<Integer>>> call();

        @RequestLine("GET " + TEST_URL)
        Mono<ResponseEntity<Mono<byte[]>>> callRaw();
    }

    public interface WrongCaller {
        @RequestLine("GET " + TEST_URL)
        Mono<ResponseEntity<List<Integer>>> call();
    }

    static <V> Map<String, V> toLowerCaseKeys(Map<String, V> map){
        Map<String, V> mapNormalized = new HashMap<>(map.size());
        map.forEach((s, o) -> mapNormalized.put(s.toLowerCase(), o));
        return mapNormalized;
    }
}
