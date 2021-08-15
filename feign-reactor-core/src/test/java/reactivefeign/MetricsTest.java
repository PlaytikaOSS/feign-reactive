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
import feign.Target;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.client.metrics.MetricsTag;
import reactivefeign.client.metrics.MicrometerReactiveLogger;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static reactivefeign.client.metrics.MetricsTag.FEIGN_CLIENT_METHOD;
import static reactivefeign.client.metrics.MicrometerReactiveLogger.DEFAULT_TIMER_NAME;

/**
 * @author Sergii Karpenko
 */
abstract public class MetricsTest extends BaseReactorTest {

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
          WireMockConfiguration.wireMockConfig()
                  .asynchronousResponseEnabled(true)
                  .dynamicPort());

  private SimpleMeterRegistry meterRegistry;

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected Target<IcecreamServiceApi> target(){
    return new Target.HardCodedTarget<>(IcecreamServiceApi.class,
            "http://"+getHost()+":" + wireMockRule.port());
  }

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long readTimeoutInMillis);

  @Before
  public void setUp(){
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  public void shouldLogMono() throws Exception {

    IceCreamOrder order = new OrderGenerator().generate(20);

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/20"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.MAPPER.writeValueAsString(order))));

    IcecreamServiceApi client = builder()
            .addLoggerListener(buildLoggerListener())
            .target(target());

    client.findOrder(20).subscribeOn(testScheduler()).block();

    assertThat(meterRegistry.get(DEFAULT_TIMER_NAME)
            .tagKeys(FEIGN_CLIENT_METHOD.getTagName())
            .timer().count()).isEqualTo(1L);
  }

  @Test
  public void shouldLogFlux() throws Exception {

    IceCreamOrder order1 = new OrderGenerator().generate(21);
    Bill billExpected1 = Bill.makeBill(order1);

    IceCreamOrder order2 = new OrderGenerator().generate(22);
    Bill billExpected2 = Bill.makeBill(order2);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders/batch"))
            .withRequestBody(equalTo(fluxRequestBody(asList(order1, order2))))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.MAPPER.writeValueAsString(asList(billExpected1, billExpected2)))));

    IcecreamServiceApi client = builder()
            .addLoggerListener(buildLoggerListener())
            .target(target());

    client.makeOrders(Flux.just(order1, order2)).subscribeOn(testScheduler()).collectList().block();

    assertThat(meterRegistry.get(DEFAULT_TIMER_NAME)
            .tagKeys(FEIGN_CLIENT_METHOD.getTagName())
            .timer().count()).isEqualTo(1L);
  }

  @Test
  public void shouldLogNoBody() {

    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    IcecreamServiceApi client = builder()
            .addLoggerListener(buildLoggerListener())
            .target(target());

    client.ping().subscribeOn(testScheduler()).block();

    assertThat(meterRegistry.get(DEFAULT_TIMER_NAME)
            .tagKeys(FEIGN_CLIENT_METHOD.getTagName())
            .timer().count()).isEqualTo(1L);

  }

  @Test(expected = Exception.class)
  public void shouldLogTimeout() {

    int readTimeoutInMillis = 100;
    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse()
                    .withFixedDelay(readTimeoutInMillis * 2)
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    IcecreamServiceApi client = builder(readTimeoutInMillis)
            .addLoggerListener(buildLoggerListener())
            .target(IcecreamServiceApi.class,
                    "http://" + getHost() + ":" + wireMockRule.port());

    try {
      client.ping().subscribeOn(testScheduler()).block();
      fail("should throw ReadTimeoutException");
    }
    catch (Exception e) {
      assertThat(meterRegistry.get(DEFAULT_TIMER_NAME)
              .tags(MetricsTag.EXCEPTION.getTagName(), ReadTimeoutException.class.getSimpleName())
              .timer().count()).isEqualTo(1L);
      assertThat(meterRegistry.get(DEFAULT_TIMER_NAME)
              .tags(MetricsTag.STATUS.getTagName(), "-1")
              .timer().count()).isEqualTo(1L);
      throw e;
    }
  }

  private MicrometerReactiveLogger buildLoggerListener() {
    return new MicrometerReactiveLogger(
            Clock.systemUTC(), meterRegistry, DEFAULT_TIMER_NAME, EnumSet.allOf(MetricsTag.class));
  }

  protected String fluxRequestBody(List<?> list) throws JsonProcessingException {
    return TestUtils.MAPPER.writeValueAsString(list);
  }

  protected String getHost() {
    return "localhost";
  }


}
