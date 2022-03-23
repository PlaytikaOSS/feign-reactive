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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ActiveProfiles;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactivefeign.allfeatures.AllFeaturesTest;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("netty")
abstract public class AllFeaturesMvcTest extends AllFeaturesTest{

	abstract protected ReactiveFeign.Builder<AllFeaturesMvc> builder();

	@Override
	protected AllFeaturesApi buildClient(String url){
		return builder()
				.decode404()
				.contract(new SpringMvcContract(emptyList(), new DefaultFormattingConversionService()))
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

	@Test
	public void shouldReturnAllPassedParametersViaSpringQueryMap() {
		Map<String, String> paramMap = new HashMap<String, String>() {{
				put("paramKey1", "paramValue1");
			    put("paramKey2", "paramValue2");
			}};
		Map<String, String> returned = ((AllFeaturesMvc)client).mirrorParametersViaSpringQueryMap(555,"777", paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsEntry("paramInPath", "555");
		assertThat(returned).containsEntry("paramInUrl", "777");
		assertThat(returned).containsAllEntriesOf(paramMap);
	}

	@Test
	@Override
	@Ignore
	//expanders not supported by Spring
	public void shouldExpandPathParam() {
	}
}
