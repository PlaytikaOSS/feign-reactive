/*
 * Copyright 2013-2018 the original author or authors.
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
import feign.FeignException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.publisher.retry.OutOfRetriesException;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignCircuitBreakerCustomizer;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactivefeign.spring.config.ReactiveRetryPolicies;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.retry.BasicReactiveRetryPolicy.retry;
import static reactivefeign.spring.config.cloud2.CloudClientUsingConfigurationsTests.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CloudClientUsingConfigurationsTests.Application.class, webEnvironment = WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+FOO+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+BAR+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}"
		})
@TestPropertySource("classpath:common.properties")
@DirtiesContext
public class CloudClientUsingConfigurationsTests extends BasicAutoconfigurationTest{

	public static final String BAR = "bar";
	public static final String FOO = "foo";
	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	private FooClient fooClient;

	@Autowired
	private BarClient barClient;


	@BeforeClass
	public static void setupStubs() {
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

	@Test
	public void shouldRetryAndFailOnLoadBalancer() {
		mockHttpServer.stubFor(get(urlPathMatching("/foo"))
				.willReturn(aResponse()
						.withFixedDelay(200)
						.withStatus(503)));

		StepVerifier.create(fooClient.foo())
				.expectErrorMatches(throwable -> throwable.getCause() instanceof OutOfRetriesException)
				.verify();

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(2);
	}

	@Test
	public void shouldFailButNotOnDefaultCircuitBreakerTimeoutAsItDisabledForBarClient() {
		mockHttpServer.stubFor(get(urlPathMatching("/bar"))
				.willReturn(aResponse()
						.withFixedDelay(1100)
						.withStatus(503)));

		StepVerifier.create(barClient.bar())
				.expectErrorMatches(throwable -> throwable.getCause() instanceof FeignException.ServiceUnavailable)
				.verify();

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(1);
	}

	@ReactiveFeignClient(name = FOO, configuration = FooConfiguration.class)
	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		Mono<String> foo();
	}

	@ReactiveFeignClient(name = BAR, configuration = BarConfiguration.class)
	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		Mono<String> bar();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(defaultConfiguration = DefaultConfiguration.class,
			clients = {FooClient.class, BarClient.class})
	protected static class Application {
	}

	@Configuration
	protected static class DefaultConfiguration {
	}

	@Configuration
	protected static class FooConfiguration {

		@Bean
		public ReactiveRetryPolicies retryPolicies(){
			return new ReactiveRetryPolicies.Builder()
					.retryOnNext(retry(1)).build();
		}
	}

	@Configuration
	protected static class BarConfiguration {

		@Bean
		public ReactiveFeignCircuitBreakerCustomizer<Resilience4JConfigBuilder, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultCustomizer() {
			return CIRCUIT_BREAKER_TIMEOUT_DISABLED_CONFIGURATION_CUSTOMIZER;
		}
	}
}
