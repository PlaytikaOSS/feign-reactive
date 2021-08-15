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
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.ReactiveOptions.ProxySettings;
import static reactivefeign.ReactiveOptions.ProxySettingsBuilder;
import static reactivefeign.TestUtils.MAPPER;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * @author Sergii Karpenko
 */
abstract public class OptionsTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  @Rule
  public WireMockClassRule wireMockProxyRule = new WireMockClassRule(
          wireMockConfig().dynamicPort().enableBrowserProxying(true));

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long readTimeoutInMillis);

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(boolean followRedirects);

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(ProxySettings proxySettings);

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldFailOnReadTimeout() {

    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withFixedDelay(200)));

    IcecreamServiceApi client = builder(100)
                .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
        .expectError(ReadTimeoutException.class)
        .verify();
  }

  @Test
  public void shouldFollowRedirects() throws JsonProcessingException {

    String oldOrderUrl = "/icecream/orders/redirect/1";
    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(oldOrderUrl))
            .willReturn(aResponse().withStatus(301)
                    .withHeader("Location", orderUrl)));

    IceCreamOrder orderExpected = new OrderGenerator().generate(1);
    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(orderExpected))));

    IcecreamServiceApi client = builder(true)
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrderWithRedirect(1).subscribeOn(testScheduler()))
            .expectNextMatches(equalsComparingFieldByFieldRecursively(orderExpected))
            .verifyComplete();
  }

  @Test
  public void shouldNotFollowRedirects() throws JsonProcessingException {

    String oldOrderUrl = "/icecream/orders/redirect/1";
    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(oldOrderUrl))
            .willReturn(aResponse().withStatus(301)
                    .withHeader("Location", orderUrl)));

    IceCreamOrder orderExpected = new OrderGenerator().generate(1);
    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(orderExpected))));

    IcecreamServiceApi client = builder(false)
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrderWithRedirect(1).subscribeOn(testScheduler()))
            .expectErrorMatches(throwable -> throwable instanceof FeignException
                    && ((FeignException) throwable).status() == 301)
            .verify();
  }

  @Test
  public void shouldUseProxy() throws JsonProcessingException {

    String orderUrl = "/icecream/orders/1";

    IceCreamOrder orderExpected = new OrderGenerator().generate(1);
    wireMockRule.stubFor(any(urlEqualTo(orderUrl))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(MAPPER.writeValueAsString(orderExpected))));

    IcecreamServiceApi client = builder(
            new ProxySettingsBuilder().host("localhost").port(wireMockProxyRule.port()).build())
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
            .expectNextMatches(equalsComparingFieldByFieldRecursively(orderExpected))
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockProxyRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().isBrowserProxyRequest()).isTrue();
  }
}
