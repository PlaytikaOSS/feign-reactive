/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.Target;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.client.RestClientException;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static reactivefeign.TestUtils.*;

/**
 * @author Sergii Karpenko
 */

abstract public class BasicFeaturesTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig());

  abstract protected <T> ReactiveFeignBuilder<T> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig().dynamicPort();
  }

  protected int wireMockPort(){
    return wireMockRule.port();
  }

  protected TestClient client;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    String targetUrl = getTargetUrl();
    client = this.<TestClient>builder()
        .decode404()
        .target(TestClient.class, targetUrl);
  }

  public String getTargetUrl() {
    return "http://localhost:" + wireMockPort();
  }

  @Test
  public void shouldFailOnCorruptedJson() {

    wireMockRule.stubFor(get(urlEqualTo("/corruptedJson"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"corrupted ! json")));

    Mono<TestObject> order = client.corruptedJson();

    StepVerifier.create(order)
            .expectErrorMatches(corruptedJsonError())
            .verify();
  }

  protected Predicate<Throwable> corruptedJsonError() {
    return throwable -> throwable instanceof RestClientException;
  }

  @Test
  public void shouldDecode404ToEmptyMono() {

    Mono<TestObject> emptyMono = client.decode404ToEmptyMono(123);

    StepVerifier.create(emptyMono)
        .verifyComplete();
  }

  @Test
  public void shouldDecode404ToEmptyFlux() {

    Flux<TestObject> emptyFlux = client.decode404ToEmptyFlux();

    StepVerifier.create(emptyFlux)
            .expectNextCount(0)
            .verifyComplete();
  }

  @Test
  public void shouldParseGenericJson() throws IOException {

    Map<String, Object> request = MAPPER.readValue(readJsonFromFile("/request.json"), Map.class);
    String requestJson = MAPPER.writeValueAsString(request);
    String responseJson = readJsonFromFile("/response.json");
    Map<String, Object> response = MAPPER.readValue(responseJson, Map.class);


    wireMockRule.stubFor(post(urlEqualTo("/genericJson"))
            .withRequestBody(equalTo(requestJson))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseJson)));

    Mono<Map<String, Object>> result = client.genericJson(request);
    StepVerifier.create(result)
            .expectNext(response)
            .verifyComplete();
  }

  @Test
  public void shouldPassResponseAsIs() throws JsonProcessingException {

    List<TestObject> testObjects = asList(new TestObject(1), new TestObject(2));
    wireMockRule.stubFor(get(urlEqualTo("/reactiveHttpResponse"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(testObjects))));

    Mono<ReactiveHttpResponse<Flux<TestObject>>> result = client.reactiveHttpResponse();
    StepVerifier.create(result)
            .expectNextMatches(response -> toLowerCaseKeys(response.headers())
                    .containsKey("content-type"))
            .verifyComplete();

    Flux<TestObject> flux = result.flatMapMany(ReactiveHttpResponse::body);
    StepVerifier.create(flux)
            .expectNextSequence(testObjects)
            .verifyComplete();
  }

  @Test
  public void shouldExpandUrlWithBaseUriForEmptyTarget() throws URISyntaxException, JsonProcessingException {

    TestObject testObject = new TestObject(1);
    String json = MAPPER.writeValueAsString(testObject);
    wireMockRule.stubFor(post(urlEqualTo("/expand"))
            .withRequestBody(equalTo(json))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

    EmptyTargetClient testClient = this.<EmptyTargetClient>builder()
            .target(Target.EmptyTarget.create(EmptyTargetClient.class));

    StepVerifier.create(testClient.expandUrl(new URI(getTargetUrl()), testObject)
            .subscribeOn(testScheduler()))
            .expectNext(testObject)
            .verifyComplete();
  }

  @Test
  public void shouldExpandQueryMapToQueryParameters() {

    String queryParameter = "queryParameter";
    String value = "1";
    wireMockRule.stubFor(post(urlEqualTo("/queryMap?" + queryParameter + "=" + value))
            .willReturn(aResponse().withStatus(200)));

    StepVerifier.create(client.queryMap(new HashMap<String, Object>(){{put(queryParameter, value);}})
            .subscribeOn(testScheduler()))
            .verifyComplete();
  }

  @Test
  public void shouldPassQueryParameters() {

    String queryParameter = "queryParam";
    String value1 = "1";
    String value2 = "2";
    wireMockRule.stubFor(get(urlEqualTo("?" + queryParameter + "=" + value1
            +"&"+ queryParameter + "=" + value2))
            .willReturn(aResponse().withStatus(200)));

    StepVerifier.create(client.queryParam(asList(value1, value2))
            .subscribeOn(testScheduler()))
            .verifyComplete();
  }

  @Test
  public void shouldExpandPojoToQueryParameters() {

    wireMockRule.stubFor(post(urlEqualTo("/queryPojo?field=1"))
            .willReturn(aResponse().withStatus(200)));

    StepVerifier.create(client.queryPojo(new TestObject(1))
            .subscribeOn(testScheduler()))
            .verifyComplete();
  }

  public interface TestClient {
    @RequestLine("GET /icecream/orders/{orderId}")
    Mono<TestObject> decode404ToEmptyMono(@Param("orderId") int orderId);

    @RequestLine("GET /icecream/orders")
    Flux<TestObject> decode404ToEmptyFlux();

    @RequestLine("GET /corruptedJson")
    Mono<TestObject> corruptedJson();

    @RequestLine("POST /genericJson")
    Mono<Map<String, Object>> genericJson(Map<String, Object> payload);

    @RequestLine("GET /reactiveHttpResponse")
    Mono<ReactiveHttpResponse<Flux<TestObject>>> reactiveHttpResponse();

    @RequestLine("POST /queryMap")
    Mono<Void> queryMap(@QueryMap Map<String, Object> queryParameters);

    @RequestLine("GET ?queryParam={queryParamValue}")
    Mono<Void> queryParam(@Param("queryParamValue") Iterable<String> queryParamValue);

    @RequestLine("POST /queryPojo")
    Mono<Void> queryPojo(@QueryMap TestObject queryPojo);
  }

  public interface EmptyTargetClient {
    @RequestLine("POST /expand")
    Mono<TestObject> expandUrl(URI baseUrl, TestObject data);
  }

  public static class TestObject {
    private int field;

    public TestObject() {}

    public TestObject(int field) {
      this.field = field;
    }

    public int getField() {
      return field;
    }

    @Override
    public boolean equals(Object obj){
      return ((TestObject)obj).field == field;
    }
  }

}
