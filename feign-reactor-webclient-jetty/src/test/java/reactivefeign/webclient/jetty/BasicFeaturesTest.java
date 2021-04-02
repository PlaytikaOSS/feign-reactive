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
package reactivefeign.webclient.jetty;

import feign.FeignException;
import org.junit.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Sergii Karpenko
 */
public class BasicFeaturesTest extends reactivefeign.BasicFeaturesTest {

  @Override
  protected <T> ReactiveFeign.Builder<T> builder() {
    return JettyWebReactiveFeign.builder();
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

    JettyWebReactiveFeign.Builder<TestClient> bd = JettyWebReactiveFeign
            .builder(wc);

    TestClient client = bd.target(TestClient.class, targetUrl);

    wireMockRule.stubFor(get(urlEqualTo("/corruptedJson"))
            .willReturn(aResponse().withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"corrupted ! json")));

    Mono<TestObject> order = client.corruptedJson();

    StepVerifier.create(order)
            .expectError(FeignException.class)
            .verify();
  }

}
