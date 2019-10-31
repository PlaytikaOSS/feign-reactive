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

package reactivefeign.jetty.h2c;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.Http2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactivefeign.ReactiveFeign;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.allfeatures.AllFeaturesFeignTest;

import static java.util.Collections.singleton;
import static reactivefeign.ReactivityTest.CALLS_NUMBER;
import static reactivefeign.jetty.h2c.TestUtils.builderHttp2;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@ContextConfiguration(classes={AllFeaturesTest.TestConfiguration.class})
@ActiveProfiles("jetty-h2c")
public class AllFeaturesTest extends AllFeaturesFeignTest {

	@Override
	protected ReactiveFeign.Builder<AllFeaturesFeign> builder() {
		return builderHttp2();
	}

	@Configuration
	@Profile("jetty-h2c")
	public static class TestConfiguration{

		@Bean
		public ReactiveWebServerFactory reactiveWebServerFactory(){
			JettyReactiveWebServerFactory jettyReactiveWebServerFactory = new JettyReactiveWebServerFactory();
			Http2 http2 = new Http2();
			http2.setEnabled(true);
			jettyReactiveWebServerFactory.setHttp2(http2);
			jettyReactiveWebServerFactory.setServerCustomizers(singleton(server -> {
				ServerConnector sc = (ServerConnector) server.getConnectors()[0];
				HttpConfiguration httpConfig = new HttpConfiguration();
				httpConfig.setIdleTimeout(0);
				HTTP2CServerConnectionFactory http2CFactory = new HTTP2CServerConnectionFactory(httpConfig);
				http2CFactory.setMaxConcurrentStreams(CALLS_NUMBER);
				sc.addConnectionFactory(http2CFactory);

				sc.addConnectionFactory(http2CFactory);
			}));
			return jettyReactiveWebServerFactory;
		}
	}
}
