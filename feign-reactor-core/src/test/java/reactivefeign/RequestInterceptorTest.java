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
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.client.ReactiveHttpRequestInterceptors;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactivefeign.utils.Pair;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;
import static reactivefeign.client.ReactiveHttpRequestInterceptors.addHeaders;
import static reactivefeign.testcase.IcecreamServiceApi.UPPER_HEADER_TO_REMOVE;
import static reactivefeign.utils.MultiValueMapUtils.addOrdered;

/**
 * @author Sergii Karpenko
 */
abstract public class RequestInterceptorTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
          wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  protected IcecreamServiceApi target(ReactiveFeignBuilder<IcecreamServiceApi> builder){
    return builder.target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());
  }

  @Test
  public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException {

    String orderUrl = "/icecream/orders/1";

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
                    .withHeader("Accept", equalTo("application/json"))
                    .withHeader("Authorization", equalTo("Bearer mytoken123"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(orderStr)))
            .setPriority(1);

    IcecreamServiceApi clientWithAuth = target(builder()
            .addRequestInterceptor(addHeaders(singletonList(new Pair<>("Authorization", "Bearer mytoken123")))));

    StepVerifier.create(clientWithAuth.findFirstOrder().subscribeOn(testScheduler()))
            .expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
            .verifyComplete();
  }

  @Test
  public void shouldInterceptRequestAndSetAuthHeaderFromSubscriberContext() throws JsonProcessingException {

    String orderUrl = "/icecream/orders/1";

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
                    .withHeader("Accept", equalTo("application/json"))
                    .withHeader("Authorization", equalTo("Bearer mytoken123"))
                    .willReturn(aResponse().withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(orderStr)))
            .setPriority(1);

    String authHeader = "Authorization";

    IcecreamServiceApi clientWithAuth = target(builder()
        .addRequestInterceptor(request -> {
          return Mono.deferContextual(ctx -> {
            addOrdered(request.headers(), authHeader, ctx.get(authHeader));
            request.headers().remove(UPPER_HEADER_TO_REMOVE.toLowerCase());
            return Mono.just(request);
          });
        }));

    IcecreamServiceApi clientWithAuth2 = target(builder()
            .addRequestInterceptor(request -> Mono
                    .deferContextual(Mono::just)
                    .map(ctx -> {
                      addOrdered(request.headers(), authHeader, ctx.get(authHeader));
                      request.headers().remove(UPPER_HEADER_TO_REMOVE.toLowerCase());
                      return request;
                    })));

    Mono<IceCreamOrder> firstOrder = clientWithAuth.findFirstOrder()
            .contextWrite(ctx -> ctx.put(authHeader, "Bearer mytoken123"))
            .subscribeOn(testScheduler());

    StepVerifier.create(firstOrder)
            .expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
            .verifyComplete();

    assertThat(wireMockRule.getAllServeEvents().get(0).getRequest()
            .containsHeader(UPPER_HEADER_TO_REMOVE)).isFalse();

  }

  @Test
  public void shouldInterceptRequestAndAddQueryParameters()  {

    wireMockRule.stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200)))
            .setPriority(1);

    IcecreamServiceApi client = target(builder()
            .addRequestInterceptor(ReactiveHttpRequestInterceptors.addQueries(asList(new Pair<>("1,2", "3,4")))));

    Mono<Void> result = client.passParameters("5,6", "7,8",
                    new HashMap<String, String>() {{
                      put("9,10", "11?12");
                    }})
            .subscribeOn(testScheduler());

    StepVerifier.create(result)
            .verifyComplete();

    assertThat(wireMockRule.getAllServeEvents().get(0).getRequest()
            .getUrl()).isEqualTo("/mirrorParameters/5,6?paramInUrl=7,8&9,10=11?12&1,2=3,4");
  }
}
