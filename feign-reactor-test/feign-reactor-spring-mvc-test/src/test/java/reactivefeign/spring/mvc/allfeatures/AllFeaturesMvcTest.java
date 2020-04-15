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

package reactivefeign.spring.mvc.allfeatures;

import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.test.context.ActiveProfiles;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactivefeign.allfeatures.AllFeaturesTest;
import reactivefeign.jetty.JettyReactiveFeign;
import reactor.test.StepVerifier;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("netty")
public class AllFeaturesMvcTest extends AllFeaturesTest{

	@Override
	protected AllFeaturesApi buildClient(String url){
		return JettyReactiveFeign.<AllFeaturesMvc>builder()
				.decode404()
				.contract(new SpringMvcContract())
				.target(AllFeaturesMvc.class, url);
	}

	@Override
	@Test
	public void shouldFailIfNoSubstitutionForPath(){
		StepVerifier.create(client.urlNotSubstituted())
				.expectNext("should be never called as contain not substituted element in path")
				.verifyComplete();
	}

	//TODO https://github.com/Playtika/feign-reactive/issues/186
	//should be fixed in RequestHeaderParameterProcessor
	@Override
	@Test(expected = IllegalStateException.class)
	public void shouldPassHeaderAndRequestParameterWithSameName() {
		super.shouldPassHeaderAndRequestParameterWithSameName();
	}
}
