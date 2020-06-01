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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static reactivefeign.TestUtils.MAPPER;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;

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

  protected IcecreamServiceApi client;

  private OrderGenerator generator = new OrderGenerator();
  private Map<Integer, IceCreamOrder> orders = generator.generateRange(10).stream()
      .collect(Collectors.toMap(IceCreamOrder::getId, o -> o));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    String targetUrl = getTargetUrl();
    client = this.builder()
        .decode404()
        .target(IcecreamServiceApi.class, targetUrl);
  }

  public String getTargetUrl() {
    return "http://localhost:" + wireMockPort();
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

}
