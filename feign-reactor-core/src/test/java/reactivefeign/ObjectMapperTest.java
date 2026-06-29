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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.TestUtils.MAPPER;

/**
 * @author Sergii Karpenko
 */
abstract public class ObjectMapperTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldUseCustomObjectMapper() throws JacksonException {

    ObjectMapper customObjectMapper = JsonMapper.builder()
            .findAndAddModules()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
            .withRequestBody(equalTo(customObjectMapper.writeValueAsString(order)))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(billExpected))));

    IcecreamServiceApi client = builder()
        .objectMapper(customObjectMapper)
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    Mono<Bill> bill = client.makeOrder(order);

    StepVerifier.create(bill)
            .expectNextCount(1)
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().getBodyAsString()).contains("order_timestamp");
  }

  @Test
  public void shouldAcceptConfiguredNonJsonMapperObjectMapper() throws JacksonException {

    ObjectMapper customObjectMapper = new ObjectMapper().rebuild()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
            .withRequestBody(equalTo(customObjectMapper.writeValueAsString(order)))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(billExpected))));

    IcecreamServiceApi client = builder()
            .objectMapper(customObjectMapper)
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.makeOrder(order))
            .expectNextCount(1)
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().getBodyAsString()).contains("order_timestamp");
  }

  @Test
  public void shouldUseDefaultObjectMapper() throws JacksonException {

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(billExpected))));

    IcecreamServiceApi client = builder()
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    Mono<Bill> bill = client.makeOrder(order);

    StepVerifier.create(bill)
            .expectNextCount(1)
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().getBodyAsString()).contains("orderTimestamp");
  }

}
