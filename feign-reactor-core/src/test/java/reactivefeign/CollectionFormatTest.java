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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import feign.CollectionFormat;
import feign.Param;
import feign.RequestLine;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.TestUtils.MAPPER;

/**
 * @author Sergii Karpenko
 */
abstract public class CollectionFormatTest extends BaseReactorTest {

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig().dynamicPort());

  abstract protected ReactiveFeignBuilder<TestFeign> builder();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldUseDefaultCollectionFormat()  {

    wireMockRule.stubFor(get(urlEqualTo("/default?ids=1&ids=2&ids=3"))
            .willReturn(aResponse().withStatus(200)));

    TestFeign client = builder()
            .target(TestFeign.class, "http://localhost:" + wireMockRule.port());

    Mono<Void> completion = client.callWithDefaultCollectionFormat(asList(1, 2, 3));

    StepVerifier.create(completion)
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().getAbsoluteUrl()).endsWith("ids=1&ids=2&ids=3");
  }

  @Test
  public void shouldUseCustomCollectionFormat()  {

    wireMockRule.stubFor(get(urlEqualTo("/custom?ids=1%2C2%2C3"))
            .willReturn(aResponse().withStatus(200)));

    TestFeign client = builder()
            .target(TestFeign.class, "http://localhost:" + wireMockRule.port());

    Mono<Void> completion = client.callWithCustomCollectionFormat(asList(1, 2, 3));

    StepVerifier.create(completion)
            .verifyComplete();

    List<ServeEvent> proxyEvents = wireMockRule.getAllServeEvents();
    assertThat(proxyEvents.get(0).getRequest().getAbsoluteUrl()).endsWith("ids=1%2C2%2C3");
  }

  public interface TestFeign {

    @RequestLine("GET /default")
    Mono<Void> callWithDefaultCollectionFormat(@Param("ids") List<Integer> ids);

    @RequestLine(value = "GET /custom", collectionFormat = CollectionFormat.CSV)
    Mono<Void> callWithCustomCollectionFormat(@Param("ids") List<Integer> ids);
  }

}
