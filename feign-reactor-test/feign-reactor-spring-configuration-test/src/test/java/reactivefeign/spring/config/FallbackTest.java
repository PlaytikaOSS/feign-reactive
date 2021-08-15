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
@SpringBootTest(classes = FallbackTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class FallbackTest {

	static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";
	private static final String TEST_URL = "/testUrl";
	private static final String FALLBACK_TEXT = "test fallback";
	public static final String CUSTOM = "custom";
	public static final String HEADER = "header";
	private static final WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Autowired
	TestReactiveFeignClientFallbackFactory feignClientFallbackFactory;

	@Test
	public void shouldAutoconfigureFallback() {
		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.withHeader(CUSTOM, equalTo(HEADER))
				.willReturn(aResponse()
						.withStatus(598)));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectNext(FALLBACK_TEXT)
				.verifyComplete();
	}

	@Test
	public void shouldAutoconfigureFallbackFactory() {
		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.withHeader(CUSTOM, equalTo(HEADER))
				.willReturn(aResponse()
						.withStatus(598)));

		Mono<String> result = feignClientFallbackFactory.testMethod();

		StepVerifier.create(result)
				.expectNext(FALLBACK_TEXT)
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

	@ReactiveFeignClient(name = "test-feign-client", url = "localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}",
						fallback = TestFallback.class)
	public interface TestReactiveFeignClient {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

	}

	public static class TestFallback implements TestReactiveFeignClient{

		@Override
		public Mono<String> testMethod() {
			return Mono.just(FALLBACK_TEXT);
		}
	}

	@ReactiveFeignClient(name = "test-feign-client-fallback-factory", url = "localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}",
			fallbackFactory = TestFallbackFactory.class)
	public interface TestReactiveFeignClientFallbackFactory {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

	}

	public static class TestFallbackFactory implements FallbackFactory<TestReactiveFeignClientFallbackFactory> {

		@Override
		public TestReactiveFeignClientFallbackFactory apply(Throwable throwable) {
			return () -> Mono.just(FALLBACK_TEXT);
		}
	}


	@EnableReactiveFeignClients(clients = {
			FallbackTest.TestReactiveFeignClient.class,
			FallbackTest.TestReactiveFeignClientFallbackFactory.class
	})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{

		@Bean
		public TestFallback reactiveFallback(){
			return new TestFallback();
		}

		@Bean
		public TestFallbackFactory reactiveFallbackFactory(){
			return new TestFallbackFactory();
		}

	}
}
