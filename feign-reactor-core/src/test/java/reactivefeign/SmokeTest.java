/**
 * Copyright 2018 The Feign Authors
 *
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.web.client.RestClientException;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.Flavor;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.Mixin;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static reactivefeign.TestUtils.*;

/**
 * @author Sergii Karpenko
 */

abstract public class SmokeTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig().dynamicPort();
  }

  protected int wireMockPort(){
    return wireMockRule.port();
  }

  private IcecreamServiceApi client;

  private OrderGenerator generator = new OrderGenerator();
  private Map<Integer, IceCreamOrder> orders = generator.generateRange(10).stream()
      .collect(Collectors.toMap(IceCreamOrder::getId, o -> o));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    String targetUrl = "http://localhost:" + wireMockPort();
    client = builder()
        .decode404()
        .target(IcecreamServiceApi.class, targetUrl);
  }

  @Test
  public void testSimpleGet_success() throws JsonProcessingException {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/flavors"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(MAPPER.writeValueAsString(Flavor.values()))));

    wireMockRule.stubFor(get(urlEqualTo("/icecream/mixins"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(MAPPER.writeValueAsString(Mixin.values()))));

    Flux<Flavor> flavors = client.getAvailableFlavors().subscribeOn(testScheduler());
    Flux<Mixin> mixins = client.getAvailableMixins().subscribeOn(testScheduler());

    StepVerifier.create(flavors)
        .expectNextSequence(asList(Flavor.values()))
        .verifyComplete();
    StepVerifier.create(mixins)
        .expectNextSequence(asList(Mixin.values()))
        .verifyComplete();

  }

  @Test
  public void shouldSuccessfullyCall() throws JsonProcessingException {

    IceCreamOrder orderExpected = orders.get(1);
    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(MAPPER.writeValueAsString(orderExpected))));

    Mono<IceCreamOrder> order = client.findOrder(1);

    StepVerifier.create(order)
        .expectNextMatches(equalsComparingFieldByFieldRecursively(orderExpected))
        .verifyComplete();
  }

  @Test
  public void shouldFailOnCorruptedJson() {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"corrupted ! json")));

    Mono<IceCreamOrder> order = client.findOrder(1);

    StepVerifier.create(order)
            .expectErrorMatches(corruptedJsonError())
            .verify();
  }

  protected Predicate<Throwable> corruptedJsonError() {
    return throwable -> throwable instanceof RestClientException;
  }

  @Test
  public void testFindOrder_empty() {

    Mono<IceCreamOrder> orderEmpty = client.findOrder(123);

    StepVerifier.create(orderEmpty)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testMakeOrder_success() throws JsonProcessingException {

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
        .withRequestBody(equalTo(MAPPER.writeValueAsString(order)))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(MAPPER.writeValueAsString(billExpected))));

    Mono<Bill> bill = client.makeOrder(order);

    StepVerifier.create(bill)
            .expectNextMatches(equalsComparingFieldByFieldRecursively(billExpected))
            .verifyComplete();
  }

  @Test
  public void testPayBill_success() throws JsonProcessingException {

    Bill bill = Bill.makeBill(new OrderGenerator().generate(30));

    wireMockRule.stubFor(post(urlEqualTo("/icecream/bills/pay"))
            .withRequestBody(equalTo(MAPPER.writeValueAsString(bill)))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    Mono<Void> result = client.payBill(bill);
    StepVerifier.create(result)
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


}
