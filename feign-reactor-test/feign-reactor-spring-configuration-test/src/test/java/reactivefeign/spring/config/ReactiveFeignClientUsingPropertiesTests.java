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

import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReadTimeoutException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ReactiveFeignClientUsingPropertiesTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:reactive-feign-properties.properties")
@DirtiesContext
public class ReactiveFeignClientUsingPropertiesTests {

	@Autowired
    ReactiveFeignContext context;

	@Autowired
	private ApplicationContext applicationContext;

	@Value("${local.server.port}")
	private int port = 0;

	private ReactiveFeignClientFactoryBean fooFactoryBean;

	private ReactiveFeignClientFactoryBean barFactoryBean;

	private ReactiveFeignClientFactoryBean formFactoryBean;

	public ReactiveFeignClientUsingPropertiesTests() {
		fooFactoryBean = new ReactiveFeignClientFactoryBean();
		fooFactoryBean.setName("foo");
		fooFactoryBean.setType(ReactiveFeignClientFactoryBean.class);

		barFactoryBean = new ReactiveFeignClientFactoryBean();
		barFactoryBean.setName("bar");
		barFactoryBean.setType(ReactiveFeignClientFactoryBean.class);

		formFactoryBean = new ReactiveFeignClientFactoryBean();
		formFactoryBean.setName("form");
		formFactoryBean.setType(ReactiveFeignClientFactoryBean.class);
	}

	public FooClient fooClient() {
		fooFactoryBean.setApplicationContext(applicationContext);
		return (FooClient)fooFactoryBean.reactiveFeign(context).target(FooClient.class, "http://localhost:" + this.port);
	}

	public BarClient barClient() {
		barFactoryBean.setApplicationContext(applicationContext);
		return (BarClient)barFactoryBean.reactiveFeign(context).target(BarClient.class, "http://localhost:" + this.port);
	}

	@Test
	public void testFoo() {
		String response = fooClient().foo().block();
		assertEquals("OK", response);
	}

	@Test(expected = ReadTimeoutException.class)
	public void testBar() {
		barClient().bar().block();
		fail("it should timeout");
	}

	protected interface FooClient {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		Mono<String> foo();
	}

	protected interface BarClient {

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		Mono<String> bar();
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	protected static class Application {

		@RequestMapping(method = RequestMethod.GET, value = "/foo")
		public String foo(@RequestHeader Map<String, String> headersMap) throws IllegalAccessException {
			if ("Foo".equals(headersMap.get("Foo")) &&
					"Bar".equals(headersMap.get("Bar"))) {
				return "OK";
			} else {
				throw new IllegalAccessException("It should has Foo and Bar header");
			}
		}

		@RequestMapping(method = RequestMethod.GET, value = "/bar")
		public String bar() throws InterruptedException {
			Thread.sleep(1000L);
			return "OK";
		}
	}

	public static class FooRequestInterceptor implements ReactiveHttpRequestInterceptor {
		@Override
		public ReactiveHttpRequest apply(ReactiveHttpRequest reactiveHttpRequest) {
			reactiveHttpRequest.headers().put("Foo", Collections.singletonList("Foo"));
			return reactiveHttpRequest;
		}
	}

	public static class BarRequestInterceptor implements ReactiveHttpRequestInterceptor {
		@Override
		public ReactiveHttpRequest apply(ReactiveHttpRequest reactiveHttpRequest) {
			reactiveHttpRequest.headers().put("Bar", Collections.singletonList("Bar"));
			return reactiveHttpRequest;
		}
	}

	public static class NoRetryer implements Retryer {

		@Override
		public void continueOrPropagate(RetryableException e) {
			throw e;
		}

		@Override
		public Retryer clone() {
			return this;
		}
	}

	public static class DefaultErrorDecoder extends ErrorDecoder.Default {
	}


}
