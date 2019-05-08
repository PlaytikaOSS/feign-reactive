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

import com.netflix.hystrix.HystrixCommand;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.Contract;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.client.log.DefaultReactiveLogger;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.metrics.MicrometerReactiveLogger;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.webclient.WebClientFeignCustomizer;
import reactivefeign.webclient.WebReactiveFeign;

import java.time.Clock;


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
	@ConditionalOnMissingBean
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnProperty(name = "reactive.feign.metrics.enabled", havingValue = "true")
	public MicrometerReactiveLogger metricsReactiveLogger() {
		return MicrometerReactiveLogger.basicTimer();
	}

	@Bean
	@ConditionalOnMissingBean(ignoredType = "reactivefeign.client.metrics.MicrometerReactiveLogger")
	@ConditionalOnProperty(name = "reactive.feign.logger.enabled", havingValue = "true")
	public ReactiveLoggerListener reactiveLogger() {
		return new DefaultReactiveLogger(Clock.systemUTC());
	}


	@Bean
	@Scope("prototype")
	@ConditionalOnClass(WebReactiveFeign.class)
	@ConditionalOnMissingBean(ignoredType = "reactivefeign.cloud.CloudReactiveFeign.Builder")
	public ReactiveFeignBuilder reactiveFeignBuilder(
			WebClient.Builder builder,
			@Autowired(required = false) WebClientFeignCustomizer webClientCustomizer) {
		return webClientCustomizer != null
				? WebReactiveFeign.builder(builder, webClientCustomizer)
				: WebReactiveFeign.builder(builder);
	}

	@AutoConfigureAfter(ReactiveFeignClientsConfiguration.class)
	@Configuration
	@ConditionalOnClass({HystrixCommand.class, LoadBalancerCommand.class, CloudReactiveFeign.class})
	@ConditionalOnProperty(name = "reactive.feign.cloud.enabled", havingValue = "true", matchIfMissing = true)
	protected static class ReactiveFeignClientsCloudConfiguration {

		@Bean
		@Scope("prototype")
		@ConditionalOnProperty(name = "reactive.feign.hystrix.enabled", havingValue = "true", matchIfMissing = true)
		public ReactiveFeignHystrixConfigurator reactiveFeignHystrixConfigurator(){
			return new ReactiveFeignHystrixConfigurator();
		}

		@Bean
		@Scope("prototype")
		@ConditionalOnProperty(name = "reactive.feign.ribbon.enabled", havingValue = "true", matchIfMissing = true)
		public ReactiveFeignRibbonConfigurator reactiveFeignRibbonConfigurator(){
			return new ReactiveFeignRibbonConfigurator();
		}

		@Bean
		@Primary
		@Scope("prototype")
		@ConditionalOnMissingBean
		public CloudReactiveFeign.Builder reactiveFeignCloudBuilder(
				ReactiveFeignBuilder reactiveFeignBuilder,
				@Value("${reactive.feign.hystrix.enabled:true}")
				boolean enableHystrix,
				@Value("${reactive.feign.ribbon.enabled:true}")
				boolean enableLoadBalancer) {
			CloudReactiveFeign.Builder cloudBuilder = CloudReactiveFeign.builder(reactiveFeignBuilder);
			if(enableLoadBalancer){
				cloudBuilder = cloudBuilder.enableLoadBalancer();
			}
			if(!enableHystrix){
				cloudBuilder = cloudBuilder.disableHystrix();
			}
			return cloudBuilder;
		}
	}

}
