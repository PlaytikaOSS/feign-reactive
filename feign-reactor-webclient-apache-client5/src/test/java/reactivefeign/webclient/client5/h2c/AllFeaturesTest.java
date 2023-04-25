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

package reactivefeign.webclient.client5.h2c;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.allfeatures.AllFeaturesFeignTest;
import reactivefeign.spring.server.config.TestServerConfigurations;

import static reactivefeign.ReactivityTest.timeToCompleteReactively;
import static reactivefeign.spring.server.config.TestServerConfigurations.JETTY_H2C;
import static reactivefeign.spring.server.config.TestServerConfigurations.UNDERTOW_H2C;
import static reactivefeign.webclient.client5.h2c.TestUtils.builderHttp2WithRequestTimeout;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ContextConfiguration(classes={TestServerConfigurations.class})
@ActiveProfiles(JETTY_H2C)
public class AllFeaturesTest extends AllFeaturesFeignTest {

	@Override
	protected ReactiveFeign.Builder<AllFeaturesFeign> builder() {
		return builderHttp2WithRequestTimeout(timeToCompleteReactively());
	}

	@Test
	@Override
	public void shouldMirrorStreamingBinaryBodyReactive() throws InterruptedException {
		if(getActiveProfiles().contains(UNDERTOW_H2C)){
			return;
		}
		super.shouldMirrorStreamingBinaryBodyReactive();
	}

	//Apache's  WebClient is not able to do this trick
	@Ignore
	@Test
	@Override
	public void shouldReturnFirstResultBeforeSecondSent() {
	}

	@Ignore
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {
	}

	//TODO Check later
	@Ignore
	@Test
	@Override
	public void shouldEncodePathParamWithReservedChars() {
	}

}
