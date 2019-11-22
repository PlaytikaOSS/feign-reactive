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

package reactivefeign.java11.h2c;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.allfeatures.AllFeaturesFeignTest;
import reactivefeign.spring.server.config.TestServerConfigurations;

import static reactivefeign.java11.h2c.TestUtils.builderHttp2;
import static reactivefeign.spring.server.config.TestServerConfigurations.UNDERTOW_H2C;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ContextConfiguration(classes={TestServerConfigurations.class})
@ActiveProfiles(UNDERTOW_H2C)
public class AllFeaturesTest extends AllFeaturesFeignTest {

	@Value("${spring.profiles.active}")
	private String activeProfile;

	@Override
	protected ReactiveFeign.Builder<AllFeaturesFeign> builder() {
		return builderHttp2();
	}

	@Test
	@Override
	public void shouldMirrorStreamingBinaryBodyReactive() throws InterruptedException {
		if(activeProfile.equals(UNDERTOW_H2C)){
			return;
		}
		super.shouldMirrorStreamingBinaryBodyReactive();
	}


	//Java 11 HttpClient is not able to do this trick
	@Ignore
	@Override
	@Test
	public void shouldReturnFirstResultBeforeSecondSent() {}

	//Java 11 HttpClient is not able to do this
	@Ignore
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {
	}
}
