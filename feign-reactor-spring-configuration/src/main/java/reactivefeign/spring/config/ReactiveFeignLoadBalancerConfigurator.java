/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package reactivefeign.spring.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud2.CloudReactiveFeign;
import reactivefeign.retry.ReactiveRetryPolicy;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static reactivefeign.spring.config.ReactiveFeignBasicConfigurator.configureRetryPolicyFromProperties;

public class ReactiveFeignLoadBalancerConfigurator extends AbstractReactiveFeignConfigurator{

	protected ReactiveFeignLoadBalancerConfigurator() {
		super(2);
	}

	@Override
	public ReactiveFeignBuilder configure(
			ReactiveFeignBuilder builder,
			ReactiveFeignNamedContext namedContext){
		if (!(builder instanceof CloudReactiveFeign.Builder)) {
			throw new IllegalArgumentException("CloudReactiveFeign.Builder expected");
		}

		CloudReactiveFeign.Builder cloudBuilder = (CloudReactiveFeign.Builder) builder;

		ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory
				= namedContext.get(ReactiveLoadBalancer.Factory.class);

		cloudBuilder = cloudBuilder.enableLoadBalancer(loadBalancerFactory);

		ReactiveRetryPolicy retryOnSame = getRetry(namedContext,
				properties -> getRetryOnSameUsingProperties(namedContext, properties),
				this::getRetryOnSameUsingConfiguration);
		if(retryOnSame != null) {
			cloudBuilder = cloudBuilder.retryOnSame(retryOnSame);
		}

		ReactiveRetryPolicy retryOnNext = getRetry(namedContext,
				properties -> getRetryOnNextUsingProperties(namedContext, properties),
				this::getRetryOnNextUsingConfiguration);
		if(retryOnNext != null) {
			cloudBuilder = cloudBuilder.retryOnNext(retryOnNext);
		}


		return cloudBuilder;
	}

	private ReactiveRetryPolicy getRetry(
			ReactiveFeignNamedContext namedContext,
			Function<ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>, ReactiveRetryPolicy> retryPropertiesFunction,
			Function<ReactiveFeignNamedContext, ReactiveRetryPolicy> retryConfigFunction){
		Stream<ReactiveRetryPolicy> retryPolicyFromConfig = Stream.of(retryConfigFunction.apply(namedContext));
		Stream<ReactiveRetryPolicy> retryPoliciesFromProperties = namedContext.getConfigs().stream()
				.map(retryPropertiesFunction);
		Stream<ReactiveRetryPolicy> reactiveRetryPolicyStream;
		if (namedContext.getProperties().isDefaultToProperties()) {
			reactiveRetryPolicyStream = Stream.concat(retryPoliciesFromProperties, retryPolicyFromConfig);
		} else {
			reactiveRetryPolicyStream = Stream.concat(retryPolicyFromConfig, retryPoliciesFromProperties);
		}

		return reactiveRetryPolicyStream
				.filter(Objects::nonNull)
				.findFirst().orElse(null);
	}

	private ReactiveRetryPolicy getRetryOnSameUsingProperties(
			ReactiveFeignNamedContext namedContext,
			ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?> config) {
		return config != null && config.getRetryOnSame() != null
				? configureRetryPolicyFromProperties(namedContext, config.getRetryOnSame()) : null;
	}

	private ReactiveRetryPolicy getRetryOnSameUsingConfiguration(ReactiveFeignNamedContext context) {
		ReactiveRetryPolicies policies = context.getOptional(ReactiveRetryPolicies.class);
		return policies != null ? policies.getRetryOnSame() : null;
	}

	private ReactiveRetryPolicy getRetryOnNextUsingProperties(
			ReactiveFeignNamedContext namedContext,
			ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?> config) {
		return config != null && config.getRetryOnNext() != null
				? configureRetryPolicyFromProperties(namedContext, config.getRetryOnNext()) : null;
	}

	private ReactiveRetryPolicy getRetryOnNextUsingConfiguration(ReactiveFeignNamedContext context) {
		ReactiveRetryPolicies policies = context.getOptional(ReactiveRetryPolicies.class);
		return policies != null ? policies.getRetryOnNext() : null;
	}
}
