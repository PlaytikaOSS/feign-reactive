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
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.FallbackFactory;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.publisher.retry.OutOfRetriesException;
import reactivefeign.retry.BasicReactiveRetryPolicy;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactivefeign.webclient.WebReactiveOptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.netflix.hystrix.HystrixCommandKey.Factory.asKey;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.AutoConfigurationTest.MOCK_SERVER_PORT_PROPERTY;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleConfigurationsTest.TestConfiguration.class, webEnvironment = WebEnvironment.NONE,
		properties = "ribbon.listOfServers=localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}")
@DirtiesContext
@TestPropertySource(locations = {
		"classpath:error-decoder.properties",
		"classpath:common.properties"
})
public class SampleConfigurationsTest {

	static final int VOLUME_THRESHOLD = 2;
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

	@Autowired
	private FallbackSampleClient fallbackSampleClient;

	@Autowired
	private FallbackFactorySampleClient fallbackFactorySampleClient;

	@Autowired
	private ErrorDecoderSampleClient errorDecoderSampleClient;

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
							&& throwable.getCause().getCause() instanceof OutOfRetriesException
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
				IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> fallbackSampleClient.sampleMethod()).collect(Collectors.toList()),
				objects -> objects);

		StepVerifier.create(results)
				.expectNextMatches(objects -> Stream.of(objects).allMatch(FALLBACK_VALUE::equals))
				.verifyComplete();

		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("FallbackSampleClient#sampleMethod()"))
				.isOpen())
				.isTrue();
	}

	@Test
	public void shouldReturnFallbackFromFactoryAndOpenCircuitBreaker() throws InterruptedException {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(503)));

		Mono<Object[]> results = Mono.zip(
				IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> fallbackFactorySampleClient.sampleMethod()).collect(Collectors.toList()),
				objects -> objects);

		StepVerifier.create(results)
				.expectNextMatches(objects -> Stream.of(objects).allMatch(FALLBACK_VALUE::equals))
				.verifyComplete();

		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("FallbackFactorySampleClient#sampleMethod()"))
				.isOpen())
				.isTrue();
	}

	@Test
	public void shouldNotOpenCircuitBreakerOnHystrixBadRequestException() throws InterruptedException {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(403)));

		List<Object> results = IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> {
			try {
				try {
					return errorDecoderSampleClient.sampleMethod().block();
				} finally {
					Thread.sleep(UPDATE_INTERVAL * 2);
				}
			} catch (Throwable t) {
				return t;
			}
		}).collect(Collectors.toList());

		assertThat(results.get(0)).isInstanceOf(HystrixBadRequestException.class);
		assertThat(results.get(results.size() - 1)).isInstanceOf(HystrixBadRequestException.class);

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(VOLUME_THRESHOLD + 1);
		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("ErrorDecoderSampleClient#sampleMethod()"))
				.isOpen())
				.isFalse();
	}

	@Test
	public void shouldOpenCircuitBreakerButNotWrapException() throws InterruptedException {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(503)));

		List<Object> results = IntStream.range(0, VOLUME_THRESHOLD + 1).mapToObj(i -> {
			try {
				try {
					return errorDecoderSampleClient.sampleMethod().block();
				} finally {
					Thread.sleep(UPDATE_INTERVAL * 2);
				}
			} catch (Throwable t) {
				return t;
			}
		}).collect(Collectors.toList());

		assertThat(results.get(0)).isInstanceOf(ErrorDecoder.OriginalError.class);
		assertThat(results.get(results.size() - 1)).isInstanceOf(HystrixRuntimeException.class);

		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(HystrixCircuitBreaker.Factory.getInstance(asKey("ErrorDecoderSampleClient#sampleMethod()"))
				.isOpen())
				.isTrue();
	}

	@ReactiveFeignClient(name = "rfgn-proper")
	protected interface PropertiesSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = "rfgn-configs",
			configuration = ReactiveFeignSampleConfiguration.class)
	protected interface ConfigsSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = "rfgn-fallback",
			fallback = ReactiveFeignFallbackConfiguration.Fallback.class,
			configuration = ReactiveFeignFallbackConfiguration.class)
	protected interface FallbackSampleClient {
		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = "rfgn-fallback-factory",
			fallbackFactory = TestFallbackFactory.class)
	protected interface FallbackFactorySampleClient {
		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	private static class TestFallbackFactory implements FallbackFactory<FallbackFactorySampleClient>{

		@Override
		public FallbackFactorySampleClient apply(Throwable throwable) {
			return () -> Mono.just(FALLBACK_VALUE);
		}
	}

	@ReactiveFeignClient(name = "rfgn-errordecoder")
	protected interface ErrorDecoderSampleClient {
		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(defaultConfiguration = DefaultConfiguration.class,
			clients = {PropertiesSampleClient.class,
					ConfigsSampleClient.class,
					FallbackSampleClient.class,
					FallbackFactorySampleClient.class,
					ErrorDecoderSampleClient.class})
	protected static class TestConfiguration {
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
	protected static class ReactiveFeignFallbackConfiguration {

		@Bean
		public Fallback fallback(){
			return new Fallback();
		}

		class Fallback implements FallbackSampleClient {
			@Override
			public Mono<String> sampleMethod() {
				return Mono.just(FALLBACK_VALUE);
			}
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
	}

	@BeforeClass
	public static void setupStubs() {
		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Before
	public void reset() throws InterruptedException {
		Hystrix.reset();
		//to close circuit breaker
		Thread.sleep(SLEEP_WINDOW);
		mockHttpServer.resetAll();
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

}
