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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignCircuitBreakerCustomizer;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static reactivefeign.spring.config.cloud2.AutoConfigurationTest.*;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AutoConfigurationTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+ TEST_FEIGN_CLIENT+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+TEST_FEIGN_CLIENT_W_PARAM+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}"
		})
@TestPropertySource("classpath:common.properties")
@DirtiesContext
public class AutoConfigurationTest {

	static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";

	static final String TEST_FEIGN_CLIENT = "test-feign-client";
	static final String TEST_FEIGN_CLIENT_W_PARAM = "test-feign-client-w-param";
	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";

	static final Customizer<ReactiveResilience4JCircuitBreakerFactory> SHORT_TIMEOUT_CUSTOMIZER = factory ->
			factory.configureDefault(
					id -> new Resilience4JConfigBuilder(id)
							.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
							.timeLimiterConfig(TimeLimiterConfig.custom()
									.timeoutDuration(Duration.ofMillis(100)).build()).build());

	static final ReactiveFeignCircuitBreakerCustomizer<Resilience4JConfigBuilder, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration>
			CIRCUIT_BREAKER_TIMEOUT_DISABLED_CONFIGURATION_CUSTOMIZER
			= configBuilder -> configBuilder
			.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
			.timeLimiterConfig(TimeLimiterConfig.custom()
					.timeoutDuration(Duration.ofSeconds(100)).build());

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Autowired
	TestReactiveFeignClientWithParamInPath feignClientWithParamInPath;

	@Test
	public void shouldMirrorIntegerStreamBody() {
		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();
	}

	@Test
	public void shouldFailOnDefaultCircuitBreakerTimeout() {
		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.willReturn(aResponse()
						.withFixedDelay(2000)
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test
	public void shouldUseParameterFromPath() {
		mockHttpServer.stubFor(get(urlPathMatching("/test/1"+TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClientWithParamInPath.testMethod(1);

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();
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

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT)
	public interface TestReactiveFeignClient {
		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();
	}

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT_W_PARAM, path = "test/{id}",
			configuration = CircuitBreakerTimeoutDisabledConfiguration.class)
	public interface TestReactiveFeignClientWithParamInPath {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod(@PathVariable("id") long id);
	}

	@EnableReactiveFeignClients(clients = {
			AutoConfigurationTest.TestReactiveFeignClient.class,
			TestReactiveFeignClientWithParamInPath.class})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{
		@Bean
		public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
			return SHORT_TIMEOUT_CUSTOMIZER;
		}
	}

	@Configuration
	protected static class CircuitBreakerTimeoutDisabledConfiguration {

		@Bean
		public ReactiveFeignCircuitBreakerCustomizer<Resilience4JConfigBuilder, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultCustomizer() {
			return CIRCUIT_BREAKER_TIMEOUT_DISABLED_CONFIGURATION_CUSTOMIZER;
		}
	}
}
