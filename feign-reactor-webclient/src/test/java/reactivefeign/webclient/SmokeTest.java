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
package reactivefeign.webclient;

import feign.FeignException;
import org.junit.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Sergii Karpenko
 */
public class SmokeTest extends reactivefeign.SmokeTest {

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
    return WebReactiveFeign.builder();
  }

  @Override
  protected Predicate<Throwable> corruptedJsonError() {
    return throwable -> throwable instanceof DecodingException;
  }
  
  @Test
  public void shouldDecodeResponseBodyWhenNoCodecsAvailable() {

    String targetUrl = "http://localhost:" + wireMockPort();

    WebClient.Builder wc = WebClient
            .builder()
            .exchangeStrategies(ExchangeStrategies.empty().build());

    WebReactiveFeign.Builder<IcecreamServiceApi> bd = WebReactiveFeign
            .builder(wc);

    IcecreamServiceApi client = bd.target(IcecreamServiceApi.class, targetUrl);

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
            .willReturn(aResponse().withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"corrupted ! json")));

    Mono<IceCreamOrder> order = client.findOrder(1);

    StepVerifier.create(order)
            .expectError(FeignException.class)
            .verify();
  }

}
