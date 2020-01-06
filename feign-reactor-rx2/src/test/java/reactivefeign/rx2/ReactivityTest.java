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
package reactivefeign.rx2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.rx2.testcase.IcecreamServiceApi;
import reactivefeign.rx2.testcase.domain.IceCreamOrder;
import reactivefeign.rx2.testcase.domain.OrderGenerator;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.waitAtMost;

/**
 * @author Sergii Karpenko
 */
public class ReactivityTest {

  public static final int DELAY_IN_MILLIS = 500;
  public static final int CALLS_NUMBER = 500;
  public static final int REACTIVE_GAIN_RATIO = 50;

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig()
          .asynchronousResponseEnabled(true)
          .dynamicPort());

  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(){
    return Rx2ReactiveFeign.builder();
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

    //TODO temporary while jetty concurrent servlet initialization fixed
    client.findFirstOrder().blockingGet();

    new Thread(() -> {
      for (int i = 0; i < CALLS_NUMBER; i++) {
        client.findFirstOrder()
            .doOnSuccess(order -> counter.incrementAndGet())
            .subscribe();
      }
    }).start();

    int timeToCompleteReactively = CALLS_NUMBER * DELAY_IN_MILLIS / REACTIVE_GAIN_RATIO;
    waitAtMost(timeToCompleteReactively, TimeUnit.MILLISECONDS)
        .until(() -> counter.get() == CALLS_NUMBER);
  }
}
