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
package reactivefeign.rx3;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.Request;
import feign.RetryableException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import reactivefeign.rx3.testcase.IcecreamServiceApi;

import java.nio.charset.Charset;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static reactivefeign.rx3.client.statushandler.Rx3StatusHandlers.throwOnStatus;
import static reactivefeign.utils.FeignUtils.httpMethod;
import static reactivefeign.utils.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static reactivefeign.utils.HttpStatus.SC_UNAUTHORIZED;

/**
 * @author Sergii Karpenko
 */
public class StatusHandlerTest {

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  protected Rx3ReactiveFeign.Builder<IcecreamServiceApi> builder(){
    return Rx3ReactiveFeign.builder();
  }

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  @Test
  public void shouldThrowRetryException() throws InterruptedException {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_SERVICE_UNAVAILABLE)));
    IcecreamServiceApi client = builder()
        .statusHandler(throwOnStatus(
            status -> status == SC_SERVICE_UNAVAILABLE,
            (methodTag, response) -> {
              Request.HttpMethod httpMethod = httpMethod(response.request().method());
              Request request = Request.create(httpMethod, response.request().uri().toString(),
                      Collections.emptyMap(), new byte[0], Charset.defaultCharset());
              return new RetryableException(
                      response.status(),
                      "Should retry on next node",
                      httpMethod, (Long)null, request);
            }))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    client.findFirstOrder().test()
            .await()
            .assertError(RetryableException.class);
  }

  @Test
  public void shouldThrowOnStatusCode() throws InterruptedException {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/2"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)));


    IcecreamServiceApi client = builder()
        .statusHandler(
            throwOnStatus(
                status -> status == SC_UNAUTHORIZED,
                (methodTag, response) -> new RuntimeException("Should login", null)))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    client.findOrder(2).test()
            .await()
            .assertError(RuntimeException.class);
  }
}
