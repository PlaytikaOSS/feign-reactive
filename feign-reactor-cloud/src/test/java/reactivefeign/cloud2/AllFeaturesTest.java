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

package reactivefeign.cloud2;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.test.context.ActiveProfiles;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactivefeign.allfeatures.AllFeaturesController;
import reactivefeign.allfeatures.AllFeaturesFeign;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@SpringBootTest(
		properties = {"spring.main.web-application-type=reactive"},
		classes = {AllFeaturesController.class, reactivefeign.allfeatures.AllFeaturesTest.TestConfiguration.class },
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableAutoConfiguration(exclude = {ReactiveWebSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class, ReactiveManagementWebSecurityAutoConfiguration.class})
@ActiveProfiles("netty")
public class AllFeaturesTest extends reactivefeign.allfeatures.AllFeaturesTest {

	private static final String serviceName = "testServiceName";

	private static ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;
	private static ReactiveCircuitBreakerFactory circuitBreakerFactory;

	@BeforeAll
	public static void setupServersList() {
		loadBalancerFactory = LoadBalancingReactiveHttpClientTest.loadBalancerFactory(serviceName, 8080);
		circuitBreakerFactory = new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null, new org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties());
	}

	@Override
	protected AllFeaturesApi buildClient() {
		return BuilderUtils.<AllFeaturesFeign>cloudBuilderWithExecutionTimeoutDisabled(circuitBreakerFactory, null)
				.enableLoadBalancer(loadBalancerFactory)
				.decode404()
				.target(AllFeaturesFeign.class, serviceName, "http://"+serviceName);
	}

	@Override
	protected AllFeaturesApi buildClient(String url) {
		throw new UnsupportedOperationException();
	}

	@Test
	@Override
	public void shouldFailIfNoSubstitutionForPath() {
		// Without the circuit breaker this would throw IllegalArgumentException. The CircuitBreaker
		// catches the exception from the method handler and (since there is no fallback configured)
		// propagates a NoFallbackAvailableException downstream.
		assertThrows(NoFallbackAvailableException.class, () ->
				client.urlNotSubstituted().subscribeOn(testScheduler()).block());
	}

	//Netty's WebClient is not able to do this trick
	@Disabled
	@Test
	@Override
	public void shouldReturnFirstResultBeforeSecondSent() {
	}

	//WebClient is not able to do this
	@Disabled
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {
	}

}
