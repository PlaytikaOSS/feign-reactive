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
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactivefeign.client.DelegatingReactiveHttpResponse;
import reactivefeign.testcase.IcecreamServiceApi;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static reactivefeign.utils.HttpStatus.SC_NOT_IMPLEMENTED;

/**
 * @author Sergii Karpenko
 */
abstract public class ResponseMapperTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldInterceptErrorResponseAndDecodeError() {

    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_NOT_IMPLEMENTED)
                .withHeader("Content-Type", "application/json")
                .withBody("Error code: XTRW")));


    IcecreamServiceApi clientWithoutAuth = builder()
        .statusHandler(null)
        .responseMapper(response -> {
          if(response.status() == SC_NOT_IMPLEMENTED){
            return Mono.just(new DelegatingReactiveHttpResponse<Publisher<?>>(response){
              @Override
              public Publisher<?> body() {
                return getResponse().bodyData().map(bytes -> {
                  String errorText = new String(bytes);
                  return new RuntimeException(errorText.substring(12));
                }).flatMap(Mono::error);
              }
            });
          } else {
            return Mono.just(response);
          }
        })
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(clientWithoutAuth.findFirstOrder().subscribeOn(testScheduler()))
        .expectErrorMatches(errorPredicate())
        .verify();
  }

  protected Predicate<Throwable> errorPredicate() {
    return throwable -> throwable.getMessage().equals("XTRW");
  }

}
