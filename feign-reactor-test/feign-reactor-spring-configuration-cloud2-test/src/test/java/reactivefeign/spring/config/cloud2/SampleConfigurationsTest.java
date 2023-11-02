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
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
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
import reactivefeign.publisher.retry.OutOfRetriesException;
import reactivefeign.retry.BasicReactiveRetryPolicy;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactivefeign.spring.config.ReactiveRetryPolicies;
import reactivefeign.webclient.WebReactiveOptions;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.cloud2.SampleConfigurationsTest.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleConfigurationsTest.TestConfiguration.class, webEnvironment = WebEnvironment.NONE,
		properties = {
				"spring.cloud.discovery.client.simple.instances."+RFGN_PROPER+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+RFGN_CONFIGS+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+RFGN_FALLBACK+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
				"spring.cloud.discovery.client.simple.instances."+RFGN_ERRORDECODER+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}"
		})
@DirtiesContext
@TestPropertySource(locations = {
		"classpath:error-decoder.properties",
		"classpath:common.properties"
})
public class SampleConfigurationsTest extends BasicAutoconfigurationTest{

	public static final String RFGN_PROPER = "rfgn-proper";
	public static final String RFGN_CONFIGS = "rfgn-configs";
	public static final String RFGN_FALLBACK = "rfgn-fallback";
	public static final String RFGN_ERRORDECODER = "rfgn-errordecoder";

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
	private ErrorDecoderSampleClient errorDecoderSampleClient;

	private static CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

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
							throwable instanceof RuntimeException
							&& throwable.getCause() instanceof OutOfRetriesException
					        && throwable.getCause().getCause() instanceof ReadTimeoutException)
					.verify();
		});
	}

	@Test
	public void shouldReturnFallbackAndOpenCircuitBreaker() {
		mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
				.willReturn(aResponse()
						.withStatus(503)));

		Mono<Object[]> results = Mono.zip(
				IntStream.range(0, VOLUME_THRESHOLD + 1)
						.mapToObj(i -> fallbackSampleClient.sampleMethod())
						.collect(Collectors.toList()),
				objects -> objects);

		StepVerifier.create(results)
				.expectNextMatches(objects -> Stream.of(objects).allMatch(FALLBACK_VALUE::equals))
				.verifyComplete();

		assertThat(circuitBreakerRegistry.circuitBreaker("FallbackSampleClient#sampleMethod()").getState())
				.isEqualTo(OPEN);
	}

	@Test
	public void shouldNotOpenCircuitBreakerOnIgnoredException() throws InterruptedException {
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

		assertThat(results.get(0)).isInstanceOf(ErrorDecoder.ClientError.class);
		assertThat(results.get(results.size() - 1)).isInstanceOf(ErrorDecoder.ClientError.class);

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(VOLUME_THRESHOLD + 1);
		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(circuitBreakerRegistry.circuitBreaker("ErrorDecoderSampleClient#sampleMethod()").getState())
				.isEqualTo(CLOSED);
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
		assertThat(results.get(results.size() - 1)).isInstanceOf(RuntimeException.class);

		//wait for circuit breaker updated its status
		Thread.sleep(UPDATE_INTERVAL);
		assertThat(circuitBreakerRegistry.circuitBreaker("ErrorDecoderSampleClient#sampleMethod()").getState())
				.isEqualTo(OPEN);
	}

	@ReactiveFeignClient(name = RFGN_PROPER)
	protected interface PropertiesSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = RFGN_CONFIGS,
			configuration = ReactiveFeignSampleConfiguration.class)
	protected interface ConfigsSampleClient {

		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = RFGN_FALLBACK,
			fallback = ReactiveFeignFallbackConfiguration.Fallback.class,
			configuration = ReactiveFeignFallbackConfiguration.class)
	protected interface FallbackSampleClient {
		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	@ReactiveFeignClient(name = RFGN_ERRORDECODER, fallbackFactory = ErrorDecoderSkipFallbackFactory.class)
	protected interface ErrorDecoderSampleClient {
		@RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
		Mono<String> sampleMethod();
	}

	protected static class ErrorDecoderSkipFallbackFactory implements FallbackFactory<ErrorDecoderSampleClient> {

        @Override
        public ErrorDecoderSampleClient apply(Throwable throwable) {
            return () -> {
                if(throwable instanceof RuntimeException) {
                	throw (RuntimeException)throwable;
				} else {
					throw Exceptions.propagate(throwable);
				}
            };
        }
    }

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(
			clients = {PropertiesSampleClient.class,
					ConfigsSampleClient.class,
					FallbackSampleClient.class,
					ErrorDecoderSampleClient.class})
	protected static class TestConfiguration {

		@Bean
		public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(){
			ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory = new ReactiveResilience4JCircuitBreakerFactory(
				CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults()
			);
			circuitBreakerFactory.configureDefault(id -> new Resilience4JConfigBuilder(id)
					.circuitBreakerConfig(CircuitBreakerConfig.custom()
							.minimumNumberOfCalls(VOLUME_THRESHOLD)
							.enableAutomaticTransitionFromOpenToHalfOpen()
							.waitDurationInOpenState(Duration.ofMillis(SLEEP_WINDOW))
							.ignoreExceptions(ErrorDecoder.ClientError.class)
							.build())
					.timeLimiterConfig(TimeLimiterConfig.custom()
							.timeoutDuration(Duration.ofSeconds(5)).build()).build());
			circuitBreakerFactory.configureCircuitBreakerRegistry(circuitBreakerRegistry);
			return circuitBreakerFactory;
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
		public ReactiveRetryPolicies retryOnNext(){
			return new ReactiveRetryPolicies.Builder()
					.retryOnSame(BasicReactiveRetryPolicy.retryWithBackoff(1, 10))
					.build();
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
		//to close circuit breaker
		Thread.sleep(SLEEP_WINDOW);
		mockHttpServer.resetAll();
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

}
