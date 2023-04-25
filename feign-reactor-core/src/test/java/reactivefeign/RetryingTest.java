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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import feign.ExceptionPropagationPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.publisher.retry.OutOfRetriesException;
import reactivefeign.retry.BasicReactiveRetryPolicy;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.Mixin;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;
import static reactivefeign.client.statushandler.ReactiveStatusHandlers.errorDecoder;
import static reactivefeign.retry.BasicReactiveRetryPolicy.retry;
import static reactivefeign.retry.FilteredReactiveRetryPolicy.notRetryOn;
import static reactivefeign.utils.HttpStatus.SC_OK;
import static reactivefeign.utils.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static wiremock.com.google.common.net.HttpHeaders.RETRY_AFTER;

/**
 * @author Sergii Karpenko
 */
public abstract class RetryingTest extends BaseReactorTest {

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
  public void shouldSuccessOnRetriesMono() throws JsonProcessingException {

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    String requestUrl = "/icecream/orders/1";
    mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
            requestUrl,
        aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER, "1"),
        aResponse().withStatus(200).withHeader("Content-Type", "application/json")
            .withBody(orderStr));

    int maxRetries = 3;
    IcecreamServiceApi client = builder()
        .retryWhen(BasicReactiveRetryPolicy.retryWithBackoff(maxRetries, 0))
        .target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
        .expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
        .verifyComplete();

    assertThat(getEventsForPath(requestUrl).size()).isEqualTo(maxRetries);
  }

  @Test
  public void shouldSuccessOnRetriesFlux() throws JsonProcessingException {

    String mixinsStr = TestUtils.MAPPER.writeValueAsString(Mixin.values());

    String requestUrl = "/icecream/mixins";
    mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
            requestUrl,
        aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER,"1"),
        aResponse().withStatus(SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(mixinsStr));

    int maxRetries = 3;
    IcecreamServiceApi client = builder()
        .retryWhen(BasicReactiveRetryPolicy.retryWithBackoff(maxRetries, 0))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.getAvailableMixins().subscribeOn(testScheduler()))
        .expectNextSequence(Arrays.asList(Mixin.values()))
        .verifyComplete();

    assertThat(getEventsForPath(requestUrl).size()).isEqualTo(maxRetries);
  }

  @Test
  public void shouldSuccessOnRetriesWoRetryAfter() throws JsonProcessingException {

    IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
    String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

    String orderUrl = "/icecream/orders/1";
    mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
            orderUrl, aResponse().withStatus(SC_SERVICE_UNAVAILABLE),
        aResponse().withStatus(SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(orderStr));

    int maxRetries = 3;
    IcecreamServiceApi client = builder()
        .retryWhen(BasicReactiveRetryPolicy.retryWithBackoff(maxRetries, 0))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
        .expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
        .verifyComplete();

    assertThat(getEventsForPath(orderUrl).size()).isEqualTo(maxRetries);
  }

  static void mockResponseAfterSeveralAttempts(WireMockClassRule rule,
                                                       int failedAttemptsNo,
                                                       String scenario,
                                                       String url,
                                                       ResponseDefinitionBuilder failResponse,
                                                       ResponseDefinitionBuilder response) {
    String state = STARTED;
    for (int attempt = 0; attempt < failedAttemptsNo; attempt++) {
      String nextState = "attempt" + attempt;
      rule.stubFor(
          get(urlEqualTo(url))
              .inScenario(scenario).whenScenarioStateIs(state)
              .willReturn(failResponse).willSetStateTo(nextState));

      state = nextState;
    }

    rule.stubFor(get(urlEqualTo(url))
        .inScenario(scenario)
        .whenScenarioStateIs(state).willReturn(response));
  }

  @Test
  public void shouldFailAsNoMoreRetries() {

    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(503).withHeader(RETRY_AFTER, "1")));

    int maxRetries = 3;
    IcecreamServiceApi client = builder()
        .retryWhen(retry(maxRetries))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
        .expectErrorMatches(throwable -> throwable instanceof OutOfRetriesException)
        .verify();

    assertThat(getEventsForPath(orderUrl).size()).isEqualTo(maxRetries + 1);
  }

  @Test
  public void shouldFailAsNoMoreRetriesWithUnwrap() {

    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse().withStatus(503).withHeader(RETRY_AFTER, "1")));

    int maxRetries = 3;
    IcecreamServiceApi client = builder()
            .statusHandler(errorDecoder((methodKey, response) -> {
              throw new BusinessException();
            }))
            .retryWhen(new BasicReactiveRetryPolicy.Builder()
                    .setMaxRetries(maxRetries)
                    .setExceptionPropagationPolicy(ExceptionPropagationPolicy.UNWRAP)
                    .build())
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
            .expectErrorMatches(throwable -> throwable instanceof BusinessException)
            .verify();

    assertThat(getEventsForPath(orderUrl).size()).isEqualTo(maxRetries + 1);
  }

  @Test
  public void shouldFailAsNoMoreRetriesWithBackoff() {

    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER, "1")));

    int maxRetries = 7;
    IcecreamServiceApi client = builder()
        .retryWhen(BasicReactiveRetryPolicy.retryWithBackoff(maxRetries, 5, testScheduler()))
        .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
        .expectErrorMatches(throwable -> throwable instanceof OutOfRetriesException)
        .verify();

    assertThat(getEventsForPath(orderUrl).size()).isEqualTo(maxRetries + 1);
  }

  @Test
  public void shouldNotRetryOnFilteredExceptions() {

    String orderUrl = "/icecream/orders/1";

    wireMockRule.stubFor(get(urlEqualTo(orderUrl))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse().withStatus(403).withHeader(RETRY_AFTER, "1")));

    IcecreamServiceApi client = builder()
            .retryWhen(notRetryOn(retry(3), IllegalArgumentException.class))
            .statusHandler(errorDecoder((methodKey, response) -> {
              throw new IllegalArgumentException();
            }))
            .target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

    StepVerifier.create(client.findOrder(1).subscribeOn(testScheduler()))
            .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException)
            .verify();

    assertThat(getEventsForPath(orderUrl).size()).isEqualTo(1);
  }

  private List<ServeEvent> getEventsForPath(String path) {
    return wireMockRule.getAllServeEvents().stream().filter(serveEvent -> serveEvent.getRequest().getUrl().contains(path)).collect(toList());
  }

  static class BusinessException extends RuntimeException {
  }


}
