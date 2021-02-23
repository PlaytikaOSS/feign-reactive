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
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import feign.MethodMetadata;
import feign.Target;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.cloud.CloudReactiveFeign;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.AutoConfigurationTest.MOCK_SERVER_PORT_PROPERTY;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AutoConfigurationTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "ribbon.listOfServers=localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}")
@TestPropertySource("classpath:common.properties")
@DirtiesContext
public class AutoConfigurationTest {

	public static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";

	public static final String TEST_FEIGN_CLIENT = "test-feign-client";
	public static final String TEST_FEIGN_CLIENT_W_PATH_PARAM = "test-feign-client-w-path-param";
	public static final String TEST_FEIGN_CLIENT_W_QUERY_PARAM = "test-feign-client-w-query-param";

	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;
	@Autowired
	TestReactiveFeignClientWithParamInPath feignClientWithParamInPath;
	@Autowired
	TestReactiveFeignClientWithParamInQuery feignClientWithParamInQuery;


	@Test
	public void shouldAutoconfigureInterceptor() {
		int counter = RequestInterceptorConfiguration.counter;

		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		assertThat(RequestInterceptorConfiguration.counter).isEqualTo(counter+1);
	}

	@Test
	public void shouldUseParameterFromPath() {
		int counter = RequestInterceptorConfiguration.counter;

		mockHttpServer.stubFor(get(urlPathMatching("/test/1"+TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClientWithParamInPath.testMethod(1);

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		assertThat(RequestInterceptorConfiguration.counter).isEqualTo(counter);
	}

	@Test
	public void shouldUseParameterFromQuery() {
		int counter = RequestInterceptorConfiguration.counter;

		mockHttpServer.stubFor(get(urlPathMatching("/test"+TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClientWithParamInQuery.testMethod(1);

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		assertThat(RequestInterceptorConfiguration.counter).isEqualTo(counter);
	}

	@BeforeClass
	public static void setup() {
		mockHttpServer.start();
		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

	@Before
	public void reset(){
		mockHttpServer.resetAll();
	}

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT,
			configuration = {
					RequestInterceptorConfiguration.class,
					HystrixTimeoutDisabledConfiguration.class})
	public interface TestReactiveFeignClient {
		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();
	}

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT_W_PATH_PARAM, path = "test/{id}",
			configuration = HystrixTimeoutDisabledConfiguration.class)
	public interface TestReactiveFeignClientWithParamInPath {
		@GetMapping(path = TEST_URL)
		Mono<String> testMethod(@PathVariable("id") long id);
	}

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT_W_QUERY_PARAM, path = "/test"+TEST_URL,
			configuration = HystrixTimeoutDisabledConfiguration.class)
	public interface TestReactiveFeignClientWithParamInQuery {
		@GetMapping
		Mono<String> testMethod(@RequestParam("id") long id);
	}

	@EnableReactiveFeignClients(clients = {
			TestReactiveFeignClient.class,
			TestReactiveFeignClientWithParamInPath.class,
			TestReactiveFeignClientWithParamInQuery.class})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{}

	@Configuration
	protected static class RequestInterceptorConfiguration {
		volatile static int counter;
		@Bean
		ReactiveHttpRequestInterceptor reactiveHttpRequestInterceptor(){
			return new ReactiveHttpRequestInterceptor() {
				@Override
				public Mono<ReactiveHttpRequest> apply(ReactiveHttpRequest reactiveHttpRequest) {
					return Mono.defer(() -> {
						counter++;
						return Mono.just(reactiveHttpRequest);
					});
				}
			};
		}
	}

	@Configuration
	protected static class HystrixTimeoutDisabledConfiguration {

		@Bean
		CloudReactiveFeign.SetterFactory setterFactory() {
			return new CloudReactiveFeign.SetterFactory() {
				@Override
				public HystrixObservableCommand.Setter create(Target<?> target, MethodMetadata methodMetadata) {
					String groupKey = target.name();
					HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(methodMetadata.configKey());
					return HystrixObservableCommand.Setter
							.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
							.andCommandKey(commandKey)
							.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
									.withExecutionTimeoutEnabled(false)
							);
				}
			};
		}
	}
}
