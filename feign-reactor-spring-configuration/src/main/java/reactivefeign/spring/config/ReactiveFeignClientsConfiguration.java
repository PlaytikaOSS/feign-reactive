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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import reactivefeign.ReactiveFeignBuilder;
//import reactivefeign.cloud.CloudReactiveFeign;
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
	@ConditionalOnMissingBean
	public ReactiveFeignBuilder reactiveFeignBuilder() {
		return WebReactiveFeign.builder();
	}

//	@Configuration
////	@ConditionalOnClass(CloudReactiveFeign.class)
////	protected static class HystrixFeignConfiguration {
////		@Bean
////		@Scope("prototype")
////		@ConditionalOnMissingBean
////		@ConditionalOnProperty(name = "reactive.feign.cloud.enabled")
////		public ReactiveFeignBuilder reactiveFeignCloudBuilder(ReactiveFeignBuilder reactiveFeignBuilder) {
////			return CloudReactiveFeign.builder(reactiveFeignBuilder);
////		}
////	}

}
