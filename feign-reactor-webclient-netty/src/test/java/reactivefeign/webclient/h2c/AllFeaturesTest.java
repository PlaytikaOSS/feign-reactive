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

package reactivefeign.webclient.h2c;

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
import reactivefeign.webclient.netty.NettyReactiveOptions;
import reactivefeign.webclient.netty.NettyWebReactiveFeign;
import reactor.netty.http.HttpProtocol;

import static reactivefeign.spring.server.config.TestServerConfigurations.JETTY_H2C;
import static reactivefeign.spring.server.config.TestServerConfigurations.UNDERTOW_H2C;
import static reactor.netty.http.HttpProtocol.H2C;

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
		return builderHttp2();
	}

	//TODO doesn't work correctly with Netty
	@Ignore
	@Test
	@Override
	public void shouldMirrorStreamingBinaryBodyReactive() throws InterruptedException {
		if(getActiveProfiles().contains(UNDERTOW_H2C)){
			return;
		}
		super.shouldMirrorStreamingBinaryBodyReactive();
	}

	//Netty WebClient is not able to do this trick
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

	//TODO doesn't work correctly with Netty
	@Ignore
	@Test
	@Override
	public void shouldMirrorBinaryBody() {
	}

	//TODO doesn't work correctly with Netty on CircleCI
	@Ignore
	@Test
	@Override
	public void shouldRunReactively() {
	}

	private static NettyWebReactiveFeign.Builder<AllFeaturesFeign> builderHttp2() {
		return NettyWebReactiveFeign.<AllFeaturesFeign>builder()
				.options(new NettyReactiveOptions.Builder()
						.setProtocols(new HttpProtocol[]{H2C})
						.build());
	}

}
