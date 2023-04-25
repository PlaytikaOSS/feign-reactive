/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.spring.config.cloud2;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import reactivefeign.publisher.retry.OutOfRetriesException;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.cloud2.LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest.FEIGN_CLIENT_TEST_LB;
import static reactivefeign.spring.config.cloud2.LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest.MOCK_SERVER_1_PORT_PROPERTY;
import static reactivefeign.spring.config.cloud2.LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest.MOCK_SERVER_2_PORT_PROPERTY;
import static reactivefeign.spring.config.cloud2.LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest.TestConfiguration;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+ FEIGN_CLIENT_TEST_LB +"[0].uri=http://localhost:${"+MOCK_SERVER_1_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+ FEIGN_CLIENT_TEST_LB +"[1].uri=http://localhost:${"+MOCK_SERVER_2_PORT_PROPERTY+"}",
		})
@TestPropertySource(locations = {
		"classpath:lb-enabled-cb-disabled.properties",
		"classpath:common.properties"})
@DirtiesContext
public class LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest {

	static final String MOCK_SERVER_1_PORT_PROPERTY = "mock.server1.port";
	static final String MOCK_SERVER_2_PORT_PROPERTY = "mock.server2.port";

	static final String FEIGN_CLIENT_TEST_LB = "feign-client-test-lb";
	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";

	private static final WireMockServer mockHttpServer1 = new WireMockServer(wireMockConfig().dynamicPort());
	private static final WireMockServer mockHttpServer2 = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldRetryAndNotFailOnDefaultCircuitBreakerTimeout() {
		Stream.of(mockHttpServer1, mockHttpServer2).forEach(wireMockServer ->
				wireMockServer.stubFor(get(urlPathMatching(TEST_URL))
						.willReturn(aResponse()
								.withFixedDelay(700)
								.withBody(BODY_TEXT)
								.withStatus(200))));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> {
					throwable.printStackTrace();
					return throwable instanceof OutOfRetriesException;
				})
				.verify();

		assertThat(mockHttpServer1.getAllServeEvents().size()).isEqualTo(1);
		assertThat(mockHttpServer2.getAllServeEvents().size()).isEqualTo(1);
	}

	@Test
	public void shouldNotFailWithLoadBalancingAndResponseEntity() {
		Stream.of(mockHttpServer1, mockHttpServer2).forEach(wireMockServer ->
				wireMockServer.stubFor(get(urlPathMatching(TEST_URL))
						.willReturn(aResponse()
								.withHeader("header1", "headerValue")
								.withBody(BODY_TEXT)
								.withStatus(200))));

		Mono<ResponseEntity<Mono<String>>> result = feignClient.testMethodResponseEntity();

		StepVerifier.create(result
						.doOnNext(response -> assertThat(response.getHeaders().containsKey("header1")).isTrue())
						.flatMapMany(HttpEntity::getBody))
				.expectNext(BODY_TEXT)
				.verifyComplete();

		assertThat(mockHttpServer1.getAllServeEvents().size() + mockHttpServer2.getAllServeEvents().size())
				.isEqualTo(1);
	}

	@BeforeClass
	public static void setUp() {
		mockHttpServer1.start();
		System.setProperty(MOCK_SERVER_1_PORT_PROPERTY, Integer.toString(mockHttpServer1.port()));

		mockHttpServer2.start();
		System.setProperty(MOCK_SERVER_2_PORT_PROPERTY, Integer.toString(mockHttpServer2.port()));
	}

	@AfterClass
	public static void tearDown() {
		mockHttpServer1.stop();
		mockHttpServer2.stop();
	}

	@Before
	public void reset(){
		mockHttpServer1.resetAll();
		mockHttpServer2.resetAll();
	}

	@ReactiveFeignClient(name = FEIGN_CLIENT_TEST_LB)
	public interface TestReactiveFeignClient {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

		@GetMapping(path = TEST_URL)
		Mono<ResponseEntity<Mono<String>>> testMethodResponseEntity();

	}

	@EnableReactiveFeignClients(clients = TestReactiveFeignClient.class)
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{}
}
