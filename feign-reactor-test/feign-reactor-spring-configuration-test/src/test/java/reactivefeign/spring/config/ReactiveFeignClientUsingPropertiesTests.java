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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReadTimeoutException;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static reactivefeign.spring.config.WebClientCustomizerTest.MOCK_SERVER_PORT_PROPERTY;
import static reactor.netty.Metrics.ACTIVE_CONNECTIONS;
import static reactor.netty.Metrics.CONNECTION_PROVIDER_PREFIX;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ReactiveFeignClientUsingPropertiesTests.Application.class, webEnvironment = WebEnvironment.NONE)
@TestPropertySource("classpath:reactive-feign-properties.properties")
@DirtiesContext
public class ReactiveFeignClientUsingPropertiesTests {

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	private FooClient fooClient;

	@Autowired
	private BarClient barClient;

	@Autowired
	private SingleDefaultHeaderClient singleDefaultHeaderClient;

	@Autowired
	private SingleDefaultQueryClient singleDefaultQueryClient;

	@Autowired
	private MultipleDefaultHeaderClient multipleSingleDefaultHeaderClient;

	@Autowired
	private MultipleDefaultQueryClient multipleSingleDefaultQueryClient;

	private MeterRegistry meterRegistry;

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

		mockHttpServer.stubFor(get(urlEqualTo("/barMetered"))
				.willReturn(aResponse()
						.withBody("OK")));

		mockHttpServer.stubFor(get(urlEqualTo("/header"))
				.withHeader("header", equalTo("value"))
				.willReturn(aResponse().withBody("OK")));

		mockHttpServer.stubFor(get(urlEqualTo("/query?query=value"))
				.willReturn(aResponse().withBody("OK")));

		mockHttpServer.stubFor(get(urlEqualTo("/headers"))
				.withHeader("header1", equalTo("value1"))
				.withHeader("header2", equalTo("value2"))
				.willReturn(aResponse().withBody("OK")));

		mockHttpServer.stubFor(get(urlEqualTo("/queries?query1=value1&query2=value2"))
				.willReturn(aResponse().withBody("OK")));

		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Before
	public void setUp(){
		meterRegistry = new SimpleMeterRegistry();
		Metrics.addRegistry(meterRegistry);
	}

	@After
	public void tearDown() {
		Metrics.removeRegistry(meterRegistry);
		meterRegistry.clear();
		meterRegistry.close();
	}

	@Test
	public void testFoo() {
		String response = fooClient.foo().block();
		assertEquals("OK", response);
	}

	@Test
    public void testDefaultSingleHeaderClient() {
        String response = singleDefaultHeaderClient.header().block();
        assertEquals("OK", response);
    }

	@Test
	public void testDefaultSingleQueryClient() {
		String response = singleDefaultQueryClient.query().block();
		assertEquals("OK", response);
	}

	@Test
	public void testDefaultMultipleHeadersClient() {
		String response = multipleSingleDefaultHeaderClient.headers().block();
		assertEquals("OK", response);
	}

	@Test
	public void testDefaultMultipleQueriesClient() {
		String response = multipleSingleDefaultQueryClient.queries().block();
		assertEquals("OK", response);
	}

	@Test(expected = ReadTimeoutException.class)
	public void testBar() {
		barClient.bar().block();
		fail("it should timeout");
	}

	@Test
	public void testBarMetered() {

		String response = barClient.barMetered()
				.doOnNext(s -> {
					Metrics.globalRegistry.forEachMeter(meter -> {
						Gauge activeConnections = meterRegistry.find(CONNECTION_PROVIDER_PREFIX + ACTIVE_CONNECTIONS).gauge();
						assertEquals(1., activeConnections.value(), 0.);
					});
				})
				.block();

		assertEquals("OK", response);
	}

	@ReactiveFeignClient(name = "foo", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		Mono<String> foo();
	}

	@ReactiveFeignClient(name = "bar", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		Mono<String> bar();

		@RequestMapping(method = RequestMethod.GET, value = "/barMetered")
		Mono<String> barMetered();
	}

	@ReactiveFeignClient(name = "header", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface SingleDefaultHeaderClient {

		@RequestMapping(method = RequestMethod.GET, value = "/header")
		Mono<String> header();
	}

	@ReactiveFeignClient(name = "query", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface SingleDefaultQueryClient {

		@RequestMapping(method = RequestMethod.GET, value = "/query")
		Mono<String> query();
	}

	@ReactiveFeignClient(name = "headers", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface MultipleDefaultHeaderClient {

		@RequestMapping(method = RequestMethod.GET, value = "/headers")
		Mono<String> headers();
	}

	@ReactiveFeignClient(name = "queries", url = "http://localhost:${" + MOCK_SERVER_PORT_PROPERTY+"}")
	protected interface MultipleDefaultQueryClient {

		@RequestMapping(method = RequestMethod.GET, value = "/queries")
		Mono<String> queries();
	}

	public static class FooRequestInterceptor implements ReactiveHttpRequestInterceptor {
		@Override
		public Mono<ReactiveHttpRequest> apply(ReactiveHttpRequest request) {
			request.headers().put("Foo", Collections.singletonList("Foo"));
			return Mono.just(request);
		}
	}

	public static class BarRequestInterceptor implements ReactiveHttpRequestInterceptor {
		@Override
		public Mono<ReactiveHttpRequest> apply(ReactiveHttpRequest request) {
			request.headers().put("Bar", Collections.singletonList("Bar"));
			return Mono.just(request);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableReactiveFeignClients(clients = {
			FooClient.class,
			BarClient.class,
			SingleDefaultHeaderClient.class,
			SingleDefaultQueryClient.class,
			MultipleDefaultHeaderClient.class,
			MultipleDefaultQueryClient.class
	})
	protected static class Application {
	}
}
