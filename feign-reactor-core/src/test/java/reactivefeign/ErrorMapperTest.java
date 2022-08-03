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
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static reactivefeign.client.statushandler.CompositeStatusHandler.compose;
import static reactivefeign.client.statushandler.ReactiveStatusHandlers.throwOnStatus;
import static reactivefeign.utils.FeignUtils.httpMethod;
import static reactivefeign.utils.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static reactivefeign.utils.HttpStatus.SC_UNAUTHORIZED;

/**
 * @author Sergii Karpenko
 */
public abstract class ErrorMapperTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  @Test
  public void shouldMapErrorToCustomExceptionOnMono() {

    IcecreamServiceApi client = builder()
            .errorMapper((reactiveHttpRequest, throwable) -> {
              if(throwable instanceof FeignException.ServiceUnavailable){
                return new UnsupportedOperationException();
              }
              return throwable;
            })
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());


    wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_SERVICE_UNAVAILABLE)));

    StepVerifier.create(client.findFirstOrder().subscribeOn(testScheduler()))
        .expectErrorMatches(customException())
        .verify();
  }

  @Test
  public void shouldMapErrorToCustomExceptionOnFlux() {

    IcecreamServiceApi client = builder()
            .errorMapper((reactiveHttpRequest, throwable) -> {
              if(throwable instanceof FeignException.ServiceUnavailable){
                return new UnsupportedOperationException();
              }
              return throwable;
            })
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());


    wireMockRule.stubFor(get(urlEqualTo("/icecream/mixins"))
            .willReturn(aResponse().withStatus(SC_SERVICE_UNAVAILABLE)));

    StepVerifier.create(client.getAvailableMixins().subscribeOn(testScheduler()))
            .expectErrorMatches(customException())
            .verify();
  }

  protected Predicate<Throwable> customException(){
    return throwable -> throwable instanceof UnsupportedOperationException;
  }
}
