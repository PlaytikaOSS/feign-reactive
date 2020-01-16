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

import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.CloudReactiveFeign;

public class ReactiveFeignHystrixConfigurator extends AbstractReactiveFeignConfigurator{

	protected ReactiveFeignHystrixConfigurator() {
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

		CloudReactiveFeign.SetterFactory setterFactory = namedContext.getOptional(CloudReactiveFeign.SetterFactory.class);
		if (setterFactory != null) {
			cloudBuilder = cloudBuilder.setHystrixCommandSetterFactory(setterFactory);
		}

		return cloudBuilder;
	}
}
