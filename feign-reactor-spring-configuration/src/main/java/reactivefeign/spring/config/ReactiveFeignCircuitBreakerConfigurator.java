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

import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud2.CloudReactiveFeign;
import reactivefeign.cloud2.ReactiveFeignCircuitBreakerFactory;

import java.util.function.Consumer;

public class ReactiveFeignCircuitBreakerConfigurator extends AbstractReactiveFeignConfigurator{

	protected ReactiveFeignCircuitBreakerConfigurator() {
		super(3);
	}

	@Override
	public ReactiveFeignBuilder configure(
            ReactiveFeignBuilder builder,
            ReactiveFeignNamedContext namedContext) {
		if (!(builder instanceof CloudReactiveFeign.Builder)) {
			throw new IllegalArgumentException("CloudReactiveFeign.Builder expected");
		}

		CloudReactiveFeign.Builder cloudBuilder = (CloudReactiveFeign.Builder) builder;

		ReactiveFeignCircuitBreakerFactory feignCircuitBreakerFactory = namedContext.getOptional(ReactiveFeignCircuitBreakerFactory.class);
		if (feignCircuitBreakerFactory != null) {
			return cloudBuilder.enableCircuitBreaker(feignCircuitBreakerFactory);
		}

		ReactiveCircuitBreakerFactory<Object, ConfigBuilder<Object>> circuitBreakerFactory
                = namedContext.getOptional(ReactiveCircuitBreakerFactory.class);
		if(circuitBreakerFactory != null){
			Consumer<ConfigBuilder<Object>> circuitBreakerCustomizer
					= namedContext.getOptional(ReactiveFeignCircuitBreakerCustomizer.class);
			if(circuitBreakerCustomizer != null){
				feignCircuitBreakerFactory = circuitBreakerId -> {
					circuitBreakerFactory.configure(circuitBreakerCustomizer, circuitBreakerId);
					return circuitBreakerFactory.create(circuitBreakerId);
				};
			} else {
				feignCircuitBreakerFactory = circuitBreakerFactory::create;
			}
			return cloudBuilder.enableCircuitBreaker(feignCircuitBreakerFactory);
		}

		return builder;
	}
}
