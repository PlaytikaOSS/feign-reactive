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
import feign.RequestLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author Sergii Karpenko
 */
public abstract class FallbackTest extends BaseReactorTest {

  public static final String fallbackValue = "fallback";
  public static final TestInterface FALLBACK = new TestInterface() {
    @Override
    public Mono<String> getMono() { return Mono.just(fallbackValue);}

    @Override
    public Flux<Integer> getFlux() { return Flux.just(1, 2, 3);}
  };

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<TestInterface> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  @Test
  public void shouldNotFailDueToFallback() {

    stubFor(get(urlEqualTo(MONO_URL))
            .willReturn(aResponse()
                    .withStatus(598)));
    stubFor(get(urlEqualTo(FLUX_URL))
            .willReturn(aResponse()
                    .withStatus(598)));


    TestInterface client = builder()
            .fallback(FALLBACK)
            .target(TestInterface.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.getMono().subscribeOn(testScheduler()))
            .expectNext(fallbackValue)
            .verifyComplete();

    StepVerifier.create(client.getFlux().subscribeOn(testScheduler()))
            .expectNext(1)
            .expectNext(2)
            .expectNext(3)
            .verifyComplete();
  }


  @Test
  public void shouldFailDueToErrorInFallback() {

    stubFor(get(urlEqualTo(MONO_URL))
            .willReturn(aResponse()
                    .withStatus(598)));

    TestInterface client = builder()
            .fallbackFactory(throwable -> new TestInterface() {
              @Override
              public Mono<String> getMono() { throw  new RuntimeException(); }

              @Override
              public Flux<Integer> getFlux() { throw  new RuntimeException(); }
            })
            .target(TestInterface.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.getMono().subscribeOn(testScheduler()))
            .expectErrorMatches(throwable -> throwable instanceof InvocationTargetException)
            .verify();
  }

  public static final String MONO_URL = "/mono";
  public static final String FLUX_URL = "/flux";

  public interface TestInterface {

    @RequestLine("GET "+MONO_URL)
    Mono<String> getMono();

    @RequestLine("GET "+FLUX_URL)
    Flux<Integer> getFlux();
  }

}
