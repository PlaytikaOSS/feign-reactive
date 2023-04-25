/**
 * Copyright 2018 The Feign Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package reactivefeign.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.Rule;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.TestUtils;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static reactivefeign.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * @author Panagiotis Rikarnto Siavelis
 */
public class SslVerificationTest extends reactivefeign.BaseReactorTest {

    @Rule
    public WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig());


    private WireMockConfiguration wireMockConfig() {
        return WireMockConfiguration.wireMockConfig()
                .httpsPort(8443)
                .httpDisabled(true);
    }

    private IcecreamServiceApi client(boolean disableSslValidation) {
        return builder(disableSslValidation)
                .target(IcecreamServiceApi.class, "https://localhost:" + wireMockRule.httpsPort());
    }

    private ReactiveFeign.Builder<IcecreamServiceApi> builder(boolean disableSslValidation) {

        return WebReactiveFeign.<IcecreamServiceApi>builder()
                .options(new WebReactiveOptions.Builder()
                        .setDisableSslValidation(disableSslValidation)
                        .build());
    }


    @Test
    public void givenDisabledSslValidation_shouldPass() throws JsonProcessingException {

        IceCreamOrder order = new OrderGenerator().generate(20);
        Bill billExpected = Bill.makeBill(order);

        wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
                .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

        IcecreamServiceApi client = client(true);
        Mono<Bill> bill = client.makeOrder(order);

        StepVerifier.create(bill.subscribeOn(testScheduler()))
                .expectNextMatches(equalsComparingFieldByFieldRecursively(billExpected))
                .verifyComplete();
    }

    @Test
    public void givenDisabledSslValidationContext_shouldPass() throws JsonProcessingException, SSLException {

        IceCreamOrder order = new OrderGenerator().generate(20);
        Bill billExpected = Bill.makeBill(order);

        wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
                .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

        IcecreamServiceApi client = WebReactiveFeign.<IcecreamServiceApi>builder()
                .options(new WebReactiveOptions.Builder()
                        .setSslContext(SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build())
                        .build())
                .target(IcecreamServiceApi.class, "https://localhost:" + wireMockRule.httpsPort());
        Mono<Bill> bill = client.makeOrder(order);

        StepVerifier.create(bill.subscribeOn(testScheduler()))
                .expectNextMatches(equalsComparingFieldByFieldRecursively(billExpected))
                .verifyComplete();
    }

    @Test
    public void givenEnabledSslValidation_shouldFail() throws JsonProcessingException {

        IceCreamOrder order = new OrderGenerator().generate(20);
        Bill billExpected = Bill.makeBill(order);

        wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
                .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));


        IcecreamServiceApi client = client(false);
        Mono<Bill> bill = client.makeOrder(order);

        StepVerifier.create(bill.subscribeOn(testScheduler()))
                .expectError()
                .verify();
    }

}
