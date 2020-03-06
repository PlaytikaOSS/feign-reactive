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
import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * Test the new capability of Reactive Feign client to support both Feign Request.Options
 * (regression) and the new ReactiveOptions configuration.
 *
 * @author Sergii Karpenko
 */

abstract public class CompressionTest extends BaseReactorTest{

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(boolean tryUseCompression);

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void testCompression() throws JsonProcessingException {

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
        .withHeader("Accept-Encoding", containing("gzip"))
        .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withHeader("Content-Encoding", "gzip")
            .withBody(Gzip.gzip(TestUtils.MAPPER.writeValueAsString(billExpected)))));

    IcecreamServiceApi client = builder(true)
                .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    Mono<Bill> bill = client.makeOrder(order);
    StepVerifier.create(bill.subscribeOn(testScheduler()))
        .expectNextMatches(equalsComparingFieldByFieldRecursively(billExpected))
        .verifyComplete();
  }
}
