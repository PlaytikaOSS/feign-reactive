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

import feign.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.client.log.DefaultReactiveLogger;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.metrics.MicrometerReactiveLogger;
import reactivefeign.java11.HttpClientFeignCustomizer;
import reactivefeign.java11.Java11ReactiveFeign;
import reactivefeign.jetty.JettyHttpClientFactory;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.webclient.WebClientFeignCustomizer;
import reactivefeign.webclient.WebReactiveFeign;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * patterned after org.springframework.cloud.netflix.feign.FeignClientsConfiguration
 */
@Configuration
public class ReactiveFeignClientsConfiguration {

	@Autowired(required = false)
	private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();

	@Autowired(required = false)
	private List<FeignFormatterRegistrar> feignFormatterRegistrars = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean
	public Contract reactiveFeignContract(
			List<AnnotatedParameterProcessor> parameterProcessors, FormattingConversionService feignConversionService) {
		return new SpringMvcContract(parameterProcessors, feignConversionService);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
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
	public FormattingConversionService feignConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		for (FeignFormatterRegistrar feignFormatterRegistrar : feignFormatterRegistrars) {
			feignFormatterRegistrar.registerFormatters(conversionService);
		}
		return conversionService;
	}

	@Configuration
	protected static class ReactiveFeignConfiguration{
		@Configuration
		@ConditionalOnClass({JettyReactiveFeign.class, org.eclipse.jetty.client.HttpClient.class})
		@ConditionalOnProperty(name = "reactive.feign.jetty", havingValue = "true")
		protected static class ReactiveFeignJettyConfiguration {

			@Bean
			@Scope("prototype")
			public ReactiveFeignBuilder reactiveFeignBuilder(
					JettyHttpClientFactory jettyHttpClientFactory) {
				return JettyReactiveFeign.builder(jettyHttpClientFactory);
			}
		}

		@Configuration
		@ConditionalOnClass({Java11ReactiveFeign.class, java.net.http.HttpClient.class})
		@ConditionalOnProperty(name = "reactive.feign.java11", havingValue = "true")
		protected static class ReactiveFeignJava11Configuration {

			@Bean
			@Scope("prototype")
			public ReactiveFeignBuilder reactiveFeignBuilder(
					@Autowired(required = false) HttpClientFeignCustomizer httpClientCustomizer) {
				return httpClientCustomizer != null
						? Java11ReactiveFeign.builder(httpClientCustomizer)
						: Java11ReactiveFeign.builder();
			}
		}

		@Configuration
		@ConditionalOnClass({WebReactiveFeign.class, WebClient.class})
		@ConditionalOnProperty(name = {"reactive.feign.jetty", "reactive.feign.java11"}, havingValue = "false", matchIfMissing = true)
		protected static class ReactiveFeignWebConfiguration {

			@Bean
			@Scope("prototype")
			public ReactiveFeignBuilder reactiveFeignBuilder(
					WebClient.Builder builder,
					@Autowired(required = false) WebClientFeignCustomizer webClientCustomizer) {
				return webClientCustomizer != null
						? WebReactiveFeign.builder(builder, webClientCustomizer)
						: WebReactiveFeign.builder(builder);
			}
		}

		@Bean
		@Scope("prototype")
		public ReactiveFeignConfigurator reactiveFeignBasicConfigurator(){
			return new ReactiveFeignBasicConfigurator();
		}
	}

	@Configuration
	@AutoConfigureAfter(ReactiveFeignConfiguration.class)
	@ConditionalOnClass({ReactiveCircuitBreakerFactory.class, ReactiveLoadBalancer.class,
			reactivefeign.cloud2.CloudReactiveFeign.class})
	@ConditionalOnProperty(name = "reactive.feign.cloud.enabled", havingValue = "true", matchIfMissing = true)
	protected static class ReactiveFeignCloud2Configuration {

		@Bean
		@Scope("prototype")
		@ConditionalOnProperty(name = "reactive.feign.circuit.breaker.enabled", havingValue = "true", matchIfMissing = true)
		public ReactiveFeignConfigurator reactiveFeignResilience4jConfigurator(){
			return new ReactiveFeignCircuitBreakerConfigurator();
		}

		@Bean
		@Scope("prototype")
		@ConditionalOnProperty(name = "reactive.feign.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
		public ReactiveFeignConfigurator reactiveFeignLoadBalancerConfigurator(){
			return new ReactiveFeignLoadBalancerConfigurator();
		}

		@Bean
		@Primary
		@Scope("prototype")
		@ConditionalOnMissingBean
		public reactivefeign.cloud2.CloudReactiveFeign.Builder reactiveFeignCloudBuilder(
				ReactiveFeignBuilder reactiveFeignBuilder) {
			return reactivefeign.cloud2.CloudReactiveFeign.builder(reactiveFeignBuilder);
		}
	}
}
