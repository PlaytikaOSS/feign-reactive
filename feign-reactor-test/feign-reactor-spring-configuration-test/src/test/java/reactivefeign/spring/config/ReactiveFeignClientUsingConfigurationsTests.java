/*
 * Copyright 2013-2018 the original author or authors.
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

package reactivefeign.spring.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.webclient.WebReactiveOptions;
import reactor.core.publisher.Mono;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static reactivefeign.spring.config.WebClientCustomizerTest.MOCK_SERVER_PORT_PROPERTY;
import static reactivefeign.spring.config.ReactiveFeignClientUsingPropertiesTests.BarRequestInterceptor;
import static reactivefeign.spring.config.ReactiveFeignClientUsingPropertiesTests.FooRequestInterceptor;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ReactiveFeignClientUsingConfigurationsTests.Application.class, webEnvironment = WebEnvironment.NONE)
@DirtiesContext
public class ReactiveFeignClientUsingConfigurationsTests {

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	private FooClient fooClient;

	@Autowired
	private BarClient barClient;


	@BeforeClass
	public static void setupStubs() {

		mockHttpServer.stubFor(get(urlEqualTo("/foo"))
				.withHeader("Foo", equalTo("Foo"))
				.withHeader("Bar", equalTo("Bar"))
				.willReturn(aResponse().withBody("OK")));

		mockHttpServer.stubFor(get(urlEqualTo("/bar"))
				.willReturn(aResponse()
						.withFixedDelay(1000)
						.withBody("OK")));

		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Test
	public void testFoo() {
		String response = fooClient.foo().block();
		assertEquals("OK", response);
	}

	@Test(expected = ReadTimeoutException.class)
	public void testBar() {
		barClient.bar().block();
		fail("it should timeout");
	}

	@ReactiveFeignClient(name = "foo", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}",
							configuration = FooConfiguration.class)
	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		Mono<String> foo();
	}

	@ReactiveFeignClient(name = "bar", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}",
			configuration = BarConfiguration.class)
	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		Mono<String> bar();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(defaultConfiguration = DefaultConfiguration.class,
			clients = {FooClient.class, BarClient.class})
	protected static class Application {
	}

	@Configuration
	protected static class DefaultConfiguration {
		@Bean
		public ReactiveOptions reactiveFeignOptions(){
			return new WebReactiveOptions.Builder()
					.setReadTimeoutMillis(5000)
					.setConnectTimeoutMillis(5000)
					.build();
		}
	}

	@Configuration
	protected static class FooConfiguration {
		@Bean
		public ReactiveHttpRequestInterceptor fooInterceptor(){
			return new FooRequestInterceptor();
		}
		@Bean
		public ReactiveHttpRequestInterceptor barInterceptor(){
			return new BarRequestInterceptor();
		}
	}

	@Configuration
	protected static class BarConfiguration {
		@Primary
		@Bean
		public ReactiveOptions options(){
			return new WebReactiveOptions.Builder()
					.setReadTimeoutMillis(500)
					.setConnectTimeoutMillis(500)
					.build();
		}
	}
}
