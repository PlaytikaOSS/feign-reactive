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

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.rx2.testcase.IcecreamServiceApi;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static reactivefeign.utils.HttpStatus.SC_NOT_FOUND;

/**
 * @author Sergii Karpenko
 */
public class NotFoundTest {

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(){
    return Rx2ReactiveFeign.builder();
  }

  @Test
  public void shouldReturnEmptyMono() throws InterruptedException {

    String orderUrl = "/icecream/orders/2";
    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_NOT_FOUND)));

    IcecreamServiceApi client = builder()
        .decode404()
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    client.findOrder(2).test()
            .await()
            .assertSubscribed()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
  }
}
