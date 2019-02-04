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

package reactivefeign.spring.config.sample;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
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
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.publisher.retry.RetryPublisherHttpClient;
import reactivefeign.retry.BasicReactiveRetryPolicy;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactivefeign.webclient.WebReactiveOptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.netflix.hystrix.HystrixCommandKey.Factory.asKey;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleConfigurationsTest.Application.class, webEnvironment = WebEnvironment.NONE)
@DirtiesContext
public class SampleConfigurationsTest {

	static final int VOLUME_THRESHOLD = 1;
	public static final String FALLBACK_VALUE = "Fallback";
	public static final int UPDATE_INTERVAL = 5;
	public static final int SLEEP_WINDOW = 1000;
	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	//configured via properties file
	@Autowired
	private PropertiesSampleClient propertiesSampleClient;

	//configured via configuration class
	@Autowired
	private ConfigsSampleClient configsSampleClient;


	//this test checks that default readTimeoutMillis is overridden for each client
	// (one in via properties file and other via configuration class)
	@Test
	public void shouldRetryAndFailOnRibbon() {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withFixedDelay(600)
						.withBody("OK")));

		asList(propertiesSampleClient, configsSampleClient).forEach(feignClient -> {
			StepVerifier.create(propertiesSampleClient.sampleMethod())
					.expectErrorMatches(throwable ->
							throwable instanceof HystrixRuntimeException
							&& throwable.getCause() instanceof ClientException
							&& throwable.getCause().getMessage().contains("Number of retries on next server exceeded")
					        && throwable.getCause().getCause() instanceof RetryPublisherHttpClient.OutOfRetriesException
							&& throwable.getCause().getCause().getCause() instanceof ReadTimeoutException)
					.verify();
		});

	}

	@Test
	public void shouldReturnFallbackAndOpenCircuitBreaker() throws InterruptedException {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(503)));

		Mono<Object[]> results = Mono.zip(
				IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> configsSampleClient.sampleMethod()).collect(Collectors.toList()),
				objects -> objects);

		StepVerifier.create(results).expectNextMatches(objects -> Stream.of(objects).allMatch(FALLBACK_VALUE::equals))
				.verifyComplete();

		//wait for circuit breaker updated its status
		Thread.sleep(5);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("ConfigsSampleClient#sampleMethod()"))
				.isOpen())
				.isTrue();
	}

	@Test
	public void shouldNotReturnFallbackAndOpenCircuitBreakerOnHystrixBadRequestException() throws InterruptedException {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(403)));

		Mono<Object[]> results = Mono.zip(
				IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> configsSampleClient.sampleMethod()).collect(Collectors.toList()),
				objects -> objects);

		StepVerifier.create(results).expectNextMatches(objects -> Stream.of(objects).allMatch(FALLBACK_VALUE::equals))
				.verifyComplete();

		//wait for circuit breaker updated its status
		Thread.sleep(5);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("ConfigsSampleClient#sampleMethod()"))
				.isOpen())
				.isTrue();

	}


	@ReactiveFeignClient(name = "rfgn-proper")
	protected interface PropertiesSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = "rfgn-configs",
			fallback = ReactiveFeignSampleConfiguration.Fallback.class,
			configuration = ReactiveFeignSampleConfiguration.class)
	protected interface ConfigsSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(defaultConfiguration = DefaultConfiguration.class,
			clients = {PropertiesSampleClient.class, ConfigsSampleClient.class})
	protected static class Application {
	}

	@Configuration
	protected static class DefaultConfiguration {

		@Bean
		public CloudReactiveFeign.SetterFactory setterFactory() {
			return new CloudReactiveFeign.SetterFactory() {
				@Override
				public HystrixObservableCommand.Setter create(Target<?> target, MethodMetadata methodMetadata) {
					return HystrixObservableCommand.Setter
							.withGroupKey(HystrixCommandGroupKey.Factory.asKey(target.name()))
							.andCommandKey(asKey(methodMetadata.configKey()))
							.andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
									//test parameter to make circuit breaker status updated frequently
									.withMetricsHealthSnapshotIntervalInMilliseconds(UPDATE_INTERVAL)
									//test parameter to make circuit breaker opened after small number of errors
									.withCircuitBreakerRequestVolumeThreshold(VOLUME_THRESHOLD)
									.withCircuitBreakerSleepWindowInMilliseconds(SLEEP_WINDOW)
									.withExecutionTimeoutInMilliseconds(5000)
							);
				}
			};
		}

	}

	@Configuration
	protected static class ReactiveFeignSampleConfiguration {

		@Bean
		public ReactiveOptions reactiveOptions(){
			return new WebReactiveOptions.Builder().setReadTimeoutMillis(500).build();
		}

		@Bean
		public ReactiveRetryPolicy reactiveRetryPolicy(){
			return BasicReactiveRetryPolicy.retryWithBackoff(1, 10);
		}

		@Bean
		public feign.codec.ErrorDecoder reactiveStatusHandler(){
			return new ErrorDecoder();
		}

		@Bean
		public Fallback fallback(){
			return new Fallback();
		}

		class Fallback implements ConfigsSampleClient {
			@Override
			public Mono<String> sampleMethod() {
				return Mono.just(FALLBACK_VALUE);
			}
		}

	}

	@BeforeClass
	public static void setupStubs() throws ClientException {
		mockHttpServer.start();

		setupLoadbalancers();
	}

	private static void setupLoadbalancers() throws ClientException {
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.loadDefaultValues();
		clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerClassName, BaseLoadBalancer.class.getName());
		ILoadBalancer lbFoo = ClientFactory.registerNamedLoadBalancerFromclientConfig("rfgn-proper", clientConfig);
		lbFoo.addServers(asList(new Server("localhost", mockHttpServer.port())));
		ILoadBalancer lbBar = ClientFactory.registerNamedLoadBalancerFromclientConfig("rfgn-configs", clientConfig);
		lbBar.addServers(asList(new Server("localhost", mockHttpServer.port())));
	}

	@Before
	public void reset() throws InterruptedException {
		//to close circuit breaker
		Thread.sleep(SLEEP_WINDOW);
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

}
