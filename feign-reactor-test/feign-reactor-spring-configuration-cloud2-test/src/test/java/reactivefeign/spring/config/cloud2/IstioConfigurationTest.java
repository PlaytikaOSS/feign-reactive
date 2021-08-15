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
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.cloud2.IstioConfigurationTest.TEST_FEIGN_CLIENT;
import static reactivefeign.spring.config.cloud2.IstioConfigurationTest.MOCK_SERVER_PORT_PROPERTY;

/**
 * @author Sergii Karpenko
 *
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = IstioConfigurationTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+ TEST_FEIGN_CLIENT+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",

				//config properties that disables LoadBalancer and CircuitBreaker and make client Istio compatible
				//see disableCircuitBreakerCustomizer
				"reactive.feign.loadbalancer.enabled = false"
		})
@TestPropertySource("classpath:common.properties")
@DirtiesContext
public class IstioConfigurationTest extends BasicAutoconfigurationTest{

	public static final String TEST_FEIGN_CLIENT = "test-feign-client";

	private static final String TEST_URL = "/testUrl";

	private static final WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldAutoconfigureInterceptor() throws InterruptedException {
		RequestInterceptorConfiguration.calls.clear();

		StepVerifier.create(
				Flux.range(0, DEFAULT_MINIMUM_NUMBER_OF_CALLS * 5)
				.flatMap(value -> feignClient.testMethod())
				.collectList()
		)
				//verify that fallback is enabled
				.expectNextMatches(results -> results.stream().allMatch(s -> s.equals(Fallback.FALLBACK)))
				.verifyComplete();

		//wait for CB to get opened
		Thread.sleep(100);

		StepVerifier.create(
				Flux.range(0, DEFAULT_MINIMUM_NUMBER_OF_CALLS * 5)
						.flatMap(value -> feignClient.testMethod())
						.collectList()
		)
				//verify that fallback is enabled
				.expectNextMatches(results -> results.stream().allMatch(s -> s.equals(Fallback.FALLBACK)))
				.verifyComplete();

		//check that CircuitBreaker is disabled and we got all requests
		assertThat(RequestInterceptorConfiguration.calls.size()).isEqualTo(DEFAULT_MINIMUM_NUMBER_OF_CALLS * 10);
		//check that LoadBalancer is disabled and we got original Urls without substitutions
		assertThat(RequestInterceptorConfiguration.calls.stream()
				.allMatch(request -> request.uri().toString().contains(TEST_FEIGN_CLIENT))).isTrue();
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
			fallback = Fallback.class,
			configuration = {RequestInterceptorConfiguration.class})
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

		@Bean
		public Customizer<ReactiveResilience4JCircuitBreakerFactory> disableCircuitBreakerCustomizer(){
			return reactiveCircuitBreakerFactory -> reactiveCircuitBreakerFactory.configureDefault(s -> {
				Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration circuitBreakerConfiguration
						= new Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration();
				circuitBreakerConfiguration.setId(s);
				circuitBreakerConfiguration.setCircuitBreakerConfig(new CircuitBreakerConfig.Builder()
						.minimumNumberOfCalls(Integer.MAX_VALUE)
						.slidingWindowType(TIME_BASED)
								.build());
				circuitBreakerConfiguration.setTimeLimiterConfig(TimeLimiterConfig.ofDefaults());
				return circuitBreakerConfiguration;
			});
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
