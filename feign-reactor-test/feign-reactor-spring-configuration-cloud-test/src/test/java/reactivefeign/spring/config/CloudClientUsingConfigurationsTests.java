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

package reactivefeign.spring.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.FeignException;
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.cloud.LoadBalancerCommandFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.AutoConfigurationTest.MOCK_SERVER_PORT_PROPERTY;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CloudClientUsingConfigurationsTests.Application.class, webEnvironment = WebEnvironment.NONE,
		properties = "ribbon.listOfServers=localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}")
@DirtiesContext
public class CloudClientUsingConfigurationsTests {

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
	public void shouldRetryAndFailOnRibbon() {
		mockHttpServer.stubFor(get(urlPathMatching("/foo"))
				.willReturn(aResponse()
						.withFixedDelay(600)
						.withStatus(503)));

		StepVerifier.create(fooClient.foo())
				.expectErrorMatches(throwable -> throwable.getCause() instanceof ClientException
		                             && throwable.getCause().getMessage().contains("Number of retries exceeded"))
				.verify();

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(2);
	}

	@Test
	public void shouldFailButNotOnDefaultHystrixTimeoutAsItDisabledForBarClient() {
		mockHttpServer.stubFor(get(urlPathMatching("/bar"))
				.willReturn(aResponse()
						.withFixedDelay(1100)
						.withStatus(503)));

		StepVerifier.create(barClient.bar())
				.expectErrorMatches(throwable -> throwable.getCause() instanceof FeignException
						&& throwable.getCause().getMessage().contains("status 503"))
				.verify();

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(1);
	}

	@ReactiveFeignClient(name = "foo", configuration = FooConfiguration.class)
	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		Mono<String> foo();
	}

	@ReactiveFeignClient(name = "bar", configuration = BarConfiguration.class)
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
		public LoadBalancerCommandFactory balancerCommandFactory(){
			return serviceName ->
					LoadBalancerCommand.builder()
							.withLoadBalancer(ClientFactory.getNamedLoadBalancer(serviceName))
							.withRetryHandler(new RequestSpecificRetryHandler(true, true,
									new DefaultLoadBalancerRetryHandler(1, 0, true), null))
							.build();
		}

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

	@Configuration
	protected static class BarConfiguration {

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
