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
import feign.FeignException;
import org.junit.ClassRule;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.rx2.testcase.IcecreamServiceApi;
import reactivefeign.rx2.testcase.domain.IceCreamOrder;
import reactivefeign.rx2.testcase.domain.OrderGenerator;
import reactivefeign.utils.Pair;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;
import static reactivefeign.client.ReactiveHttpRequestInterceptors.addHeaders;
import static reactivefeign.rx2.TestUtils.equalsComparingFieldByFieldRecursivelyRx;
import static reactivefeign.utils.HttpStatus.SC_UNAUTHORIZED;

/**
 * @author Sergii Karpenko
 */
public class RequestInterceptorTest {

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(){
    return Rx2ReactiveFeign.builder();
  }

  @Test
  public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException, InterruptedException {

    String orderUrl = "/icecream/orders/1";

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)))
        .setPriority(100);

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .withHeader("Authorization", equalTo("Bearer mytoken123"))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(orderStr)))
        .setPriority(1);

    IcecreamServiceApi clientWithoutAuth = builder()
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    clientWithoutAuth.findFirstOrder().test()
            .await()
            .assertSubscribed()
            .assertError(FeignException.class);

    IcecreamServiceApi clientWithAuth = builder()
        .addRequestInterceptor(addHeaders(singletonList(new Pair<>("Authorization", "Bearer mytoken123"))))
        .target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());

    clientWithAuth.findFirstOrder().test()
            .await()
            .assertSubscribed()
            .assertValue(equalsComparingFieldByFieldRecursivelyRx(orderGenerated))
            .assertNoErrors()
            .assertComplete();

  }
}
