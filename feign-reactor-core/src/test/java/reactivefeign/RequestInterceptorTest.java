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
import feign.FeignException;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactivefeign.utils.Pair;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonList;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;
import static reactivefeign.client.ReactiveHttpRequestInterceptors.addHeaders;

/**
 * @author Sergii Karpenko
 */
abstract public class RequestInterceptorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException {

    String orderUrl = "/icecream/orders/1";

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)))
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

    StepVerifier.create(clientWithoutAuth.findFirstOrder())
        .expectErrorMatches(notAuthorizedException())
        .verify();

    IcecreamServiceApi clientWithAuth = builder()
        .addRequestInterceptor(addHeaders(singletonList(new Pair<>("Authorization", "Bearer mytoken123"))))
        .target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());

    StepVerifier.create(clientWithAuth.findFirstOrder())
        .expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
        .verifyComplete();

  }

  protected Predicate<Throwable> notAuthorizedException() {
    return throwable -> throwable instanceof FeignException;
  }
}
