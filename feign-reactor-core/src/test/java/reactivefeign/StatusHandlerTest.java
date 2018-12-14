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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RetryableException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static reactivefeign.client.statushandler.CompositeStatusHandler.compose;
import static reactivefeign.client.statushandler.ReactiveStatusHandlers.throwOnStatus;

/**
 * @author Sergii Karpenko
 */
public abstract class StatusHandlerTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  @Test
  public void shouldThrowRetryException() {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)));
    IcecreamServiceApi client = builder()
        .statusHandler(throwOnStatus(
            status -> status == HttpStatus.SC_SERVICE_UNAVAILABLE,
            (methodTag, response) -> new RetryableException("Should retry on next node", null)))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findFirstOrder())
        .expectError(RetryableException.class);
  }

  @Test
  public void shouldThrowOnStatusCode() {
    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)));

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/2"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));


    IcecreamServiceApi client = builder()
        .statusHandler(compose(
            throwOnStatus(
                status -> status == HttpStatus.SC_SERVICE_UNAVAILABLE,
                (methodTag, response) -> new RetryableException("Should retry on next node", null)),
            throwOnStatus(
                status -> status == HttpStatus.SC_UNAUTHORIZED,
                (methodTag, response) -> new RuntimeException("Should login", null))))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findFirstOrder())
        .expectError(RetryableException.class)
        .verify();

    StepVerifier.create(client.findOrder(2))
        .expectError(RuntimeException.class)
        .verify();

  }
}
