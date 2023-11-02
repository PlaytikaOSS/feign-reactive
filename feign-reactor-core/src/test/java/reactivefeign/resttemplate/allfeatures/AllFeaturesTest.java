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

package reactivefeign.resttemplate.allfeatures;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.allfeatures.AllFeaturesFeignTest;
import reactivefeign.resttemplate.client.RestTemplateFakeReactiveFeign;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("netty")
public class AllFeaturesTest extends AllFeaturesFeignTest {

    //to not detect blocking calls
	@Override
	protected Scheduler testScheduler(){
		return Schedulers.boundedElastic();
	}

	@Override
	protected ReactiveFeign.Builder<AllFeaturesFeign> builder() {
		return RestTemplateFakeReactiveFeign.builder();
	}

	@Ignore
	@Test
	@Override
	public void shouldMirrorStreamingBinaryBodyReactive(){}

	@Ignore
	@Test
	@Override
	public void shouldMirrorBinaryBody(){}

	@Ignore
	@Test
	@Override
	public void shouldRunReactively(){}

	@Ignore
	@Test
	@Override
	public void shouldReturnFirstResultBeforeSecondSent(){}

	@Ignore
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {}

	@Ignore
	@Test
	@Override
	public void shouldMirrorIntegerStreamBody() {}
}
