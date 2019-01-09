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
import org.awaitility.Duration;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.waitAtMost;

/**
 * @author Sergii Karpenko
 */
abstract public class ReactivityTest {

  public static final int DELAY_IN_MILLIS = 500;
  public static final int CALLS_NUMBER = 500;
  public static final int REACTIVE_GAIN_RATIO = 20;

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
          wireMockConfig()
                  .asynchronousResponseEnabled(true)
                  .dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldRunReactively() throws JsonProcessingException {

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(orderStr)
                    .withFixedDelay(DELAY_IN_MILLIS)));

    IcecreamServiceApi client = builder()
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    AtomicInteger counter = new AtomicInteger();

    new Thread(() -> {
      for (int i = 0; i < CALLS_NUMBER; i++) {
        client.findFirstOrder()
                .doOnNext(order -> counter.incrementAndGet())
                .subscribe();
      }
    }).start();

    waitAtMost(new Duration(timeToCompleteReactively(), TimeUnit.MILLISECONDS))
            .until(() -> counter.get() == CALLS_NUMBER);
  }

  private int timeToCompleteReactively(){
    return CALLS_NUMBER * DELAY_IN_MILLIS / getReactiveGainRatio();
  }

  protected int getReactiveGainRatio(){
    return REACTIVE_GAIN_RATIO;
  }
}
