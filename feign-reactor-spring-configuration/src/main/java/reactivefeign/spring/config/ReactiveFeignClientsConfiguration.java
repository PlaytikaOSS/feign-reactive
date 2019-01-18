/*
 * Copyright 2013-2017 the original author or authors.
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

import com.netflix.client.RetryHandler;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.Contract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.webclient.WebReactiveFeign;


/**
 * patterned after org.springframework.cloud.netflix.feign.FeignClientsConfiguration
 */
@Configuration
public class ReactiveFeignClientsConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Contract reactiveFeignContract() {
		return new SpringMvcContract();
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean(ignoredType = "reactivefeign.cloud.CloudReactiveFeign.Builder")
	public ReactiveFeignBuilder reactiveFeignBuilder() {
		return WebReactiveFeign.builder();
	}

	@AutoConfigureAfter(ReactiveFeignClientsConfiguration.class)
	@Configuration
	@ConditionalOnClass({HystrixCommand.class, LoadBalancerCommand.class, CloudReactiveFeign.class})
	@ConditionalOnProperty(name = "reactive.feign.cloud.enabled", matchIfMissing = true)
	protected static class ReactiveFeignClientsCloudConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnMissingBean
		public RetryHandler reactiveRetryHandler(){
			//no retries
			return RetryHandler.DEFAULT;
		}

		@Bean
		@Scope("prototype")
		@ConditionalOnMissingBean
		@ConditionalOnProperty(name = "reactive.feign.hystrix.enabled", matchIfMissing = true)
		public CloudReactiveFeign.SetterFactory reactiveCommandSetterFactory(){
			//no retries
			return new CloudReactiveFeign.DefaultSetterFactory();
		}

		@Bean
		@Primary
		@Scope("prototype")
		@ConditionalOnMissingBean
		public CloudReactiveFeign.Builder reactiveFeignCloudBuilder(
				ReactiveFeignBuilder reactiveFeignBuilder,
				@Value("${reactive.feign.ribbon.enabled:true}")
				boolean enableLoadBalancer,
				RetryHandler reactiveRetryHandler,
				CloudReactiveFeign.SetterFactory reactiveCommandSetterFactory) {
			CloudReactiveFeign.Builder cloudBuilder = CloudReactiveFeign.builder(reactiveFeignBuilder);
			if(enableLoadBalancer){
				cloudBuilder = cloudBuilder.enableLoadBalancer(reactiveRetryHandler);
			}
			if(reactiveCommandSetterFactory != null){
				cloudBuilder = cloudBuilder.setHystrixCommandSetterFactory(reactiveCommandSetterFactory);
			} else {
				cloudBuilder = cloudBuilder.disableHystrix();
			}
			return cloudBuilder;
		}
	}

}
