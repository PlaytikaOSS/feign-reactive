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

package reactivefeign.spring.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import reactivefeign.FallbackFactory;
import reactivefeign.webclient.WebClientFeignCustomizer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WebClientCustomizerTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class WebClientCustomizerTest {

	static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";
	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";
	public static final String CUSTOM = "custom";
	public static final String HEADER = "header";
	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldAutoconfigureCustomWebClientCustomizer() {
		mockHttpServer.stubFor(WireMock.get(WireMock.urlPathMatching(TEST_URL))
				.withHeader(CUSTOM, equalTo(HEADER))
				.willReturn(WireMock.aResponse()
						.withBody(BODY_TEXT)));
		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();
	}

	@BeforeClass
	public static void setup() {
		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Before
	public void before(){
		mockHttpServer.resetAll();
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

	@ReactiveFeignClient(name = "test-feign-client", url = "localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}")
	public interface TestReactiveFeignClient {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

	}

	@EnableReactiveFeignClients(clients = {
			WebClientCustomizerTest.TestReactiveFeignClient.class
	})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{

		@Bean
		public WebClientFeignCustomizer webClientCustomizer(){
			return webClientBuilder -> webClientBuilder.defaultHeader(CUSTOM, HEADER);
		}
	}
}
