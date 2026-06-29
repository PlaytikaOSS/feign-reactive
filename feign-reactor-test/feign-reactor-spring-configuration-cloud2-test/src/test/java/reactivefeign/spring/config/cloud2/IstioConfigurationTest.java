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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.cloud2.ReactiveFeignCircuitBreakerFactory;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.cloud2.IstioConfigurationTest.TEST_FEIGN_CLIENT;
import static reactivefeign.spring.config.cloud2.IstioConfigurationTest.MOCK_SERVER_PORT_PROPERTY;

/**
 * @author Sergii Karpenko
 *
 */

@SpringBootTest(classes = IstioConfigurationTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+ TEST_FEIGN_CLIENT+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",

				//config property that disables LoadBalancer and makes the client Istio compatible
				"reactive.feign.loadbalancer.enabled = false"
		})
@TestPropertySource("classpath:common.properties")
@DirtiesContext
public class IstioConfigurationTest extends BasicAutoconfigurationTest{

	public static final String TEST_FEIGN_CLIENT = "test-feign-client";

	private static final String TEST_URL = "/testUrl";

	private static final int REQUEST_COUNT = 5;

	private static final WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldAutoconfigureInterceptor() {
		RequestInterceptorConfiguration.calls.clear();

		StepVerifier.create(
				Flux.range(0, REQUEST_COUNT)
				.flatMap(value -> feignClient.testMethod())
				.collectList()
		)
				//verify that fallback is enabled
				.expectNextMatches(results -> results.stream().allMatch(s -> s.equals(Fallback.FALLBACK)))
				.verifyComplete();

		StepVerifier.create(
				Flux.range(0, REQUEST_COUNT)
						.flatMap(value -> feignClient.testMethod())
						.collectList()
		)
				//verify that fallback is enabled
				.expectNextMatches(results -> results.stream().allMatch(s -> s.equals(Fallback.FALLBACK)))
				.verifyComplete();

		//check that CircuitBreaker is disabled and we got all requests
		assertThat(RequestInterceptorConfiguration.calls.size()).isEqualTo(REQUEST_COUNT * 2);
		//check that LoadBalancer is disabled and we got original Urls without substitutions
		assertThat(RequestInterceptorConfiguration.calls.stream()
				.allMatch(request -> request.uri().toString().contains(TEST_FEIGN_CLIENT))).isTrue();
	}


	@BeforeAll
	public static void setup() {
		mockHttpServer.start();
		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@AfterAll
	public static void teardown() {
		mockHttpServer.stop();
	}

	@BeforeEach
	public void reset(){
		mockHttpServer.resetAll();
	}

	@ReactiveFeignClient(name = TEST_FEIGN_CLIENT,
			fallback = Fallback.class,
			configuration = {RequestInterceptorConfiguration.class, CircuitBreakerConfiguration.class})
	public interface TestReactiveFeignClient {
		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();
	}

	public static class Fallback implements TestReactiveFeignClient{

		public static final String FALLBACK = "fallback!!!";

		@Override
		public Mono<String> testMethod() {
			return Mono.just(FALLBACK);
		}
	}

	@EnableReactiveFeignClients(clients = {
			TestReactiveFeignClient.class})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{

	}

	@Configuration
	protected static class CircuitBreakerConfiguration {
		@Bean
		public ReactiveFeignCircuitBreakerFactory passThroughCircuitBreakerFactory(){
			return circuitBreakerId -> new ReactiveCircuitBreaker() {
				@Override
				public <T> Mono<T> run(Mono<T> toRun, java.util.function.Function<Throwable, Mono<T>> fallback) {
					return toRun.onErrorResume(fallback);
				}

				@Override
				public <T> Flux<T> run(Flux<T> toRun, java.util.function.Function<Throwable, Flux<T>> fallback) {
					return toRun.onErrorResume(fallback);
				}
			};
		}
	}

	@Configuration
	protected static class RequestInterceptorConfiguration {
		static Queue<ReactiveHttpRequest> calls = new ConcurrentLinkedQueue<>();
		@Bean
		ReactiveHttpRequestInterceptor reactiveHttpRequestInterceptor(){
			return new ReactiveHttpRequestInterceptor() {
				@Override
				public Mono<ReactiveHttpRequest> apply(ReactiveHttpRequest reactiveHttpRequest) {
					return Mono.defer(() -> {
						calls.add(reactiveHttpRequest);
						return Mono.just(reactiveHttpRequest);
					});
				}
			};
		}
	}

}
