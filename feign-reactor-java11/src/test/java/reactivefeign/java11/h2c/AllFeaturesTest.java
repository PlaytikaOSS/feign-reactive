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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.allfeatures.AllFeaturesFeignTest;
import reactivefeign.spring.server.config.TestServerConfigurations;

import static reactivefeign.java11.h2c.TestUtils.builderHttp2;
import static reactivefeign.spring.server.config.TestServerConfigurations.JETTY_H2C;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@EnableAutoConfiguration(exclude = {ReactiveWebSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ContextConfiguration(classes={TestServerConfigurations.class})
@ActiveProfiles(JETTY_H2C)
public class AllFeaturesTest extends AllFeaturesFeignTest {

	@Override
	protected ReactiveFeign.Builder<AllFeaturesFeign> builder() {
		return builderHttp2();
	}

	@Test
	@Override
	public void shouldMirrorStreamingBinaryBodyReactive() throws InterruptedException {
		super.shouldMirrorStreamingBinaryBodyReactive();
	}

	// JDK HttpClient cannot observe the first streaming item before the second request item is sent
	@Disabled
	@Override
	@Test
	public void shouldReturnFirstResultBeforeSecondSent() {}

	// JDK HttpClient does not support this request-body streaming scenario
	@Disabled
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {
	}

	// Jetty H2C test server rejects encoded '/' as "Ambiguous URI path separator" by default
	// (UriCompliance.DEFAULT). Not a client bug; would require server-side UriCompliance.UNSAFE.
	@Disabled
	@Test
	@Override
	public void shouldEncodePathParamWithReservedChars() {
	}
}
