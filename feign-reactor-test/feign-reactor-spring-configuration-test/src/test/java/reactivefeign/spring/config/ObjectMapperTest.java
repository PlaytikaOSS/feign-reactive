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

package reactivefeign.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ObjectMapperTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class ObjectMapperTest {

	static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";
	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";
	private static final WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Autowired
	TestReactiveFeignClientWithCustomObjectMapper customFeignClient;

	@Test
	public void shouldAutoconfigureCustomObjectMapper() {
		mockHttpServer.stubFor(WireMock.post(WireMock.urlPathMatching(TEST_URL))
						.withRequestBody(equalTo("{\"field-with-good-name\":\"1\"}"))
				.willReturn(WireMock.aResponse()
						.withBody(BODY_TEXT)));
		Mono<String> result = customFeignClient.testMethod(new TestObject("1"));

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		List<ServeEvent> proxyEvents = mockHttpServer.getAllServeEvents();
		assertThat(proxyEvents.get(0).getRequest().getBodyAsString()).contains("field-with-good-name");
	}

	@Test
	public void shouldGlobalObjectMapper() {
		mockHttpServer.stubFor(WireMock.post(WireMock.urlPathMatching(TEST_URL))
				.withRequestBody(equalTo("{\"field_with_good_name\":\"1\"}"))
				.willReturn(WireMock.aResponse()
						.withBody(BODY_TEXT)));
		Mono<String> result = feignClient.testMethod(new TestObject("1"));

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		List<ServeEvent> proxyEvents = mockHttpServer.getAllServeEvents();
		assertThat(proxyEvents.get(0).getRequest().getBodyAsString()).contains("field_with_good_name");
	}

	@BeforeClass
	public static void setup() {
		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Before
	public void before(){
		mockHttpServer.resetAll();
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

	@ReactiveFeignClient(name = "test-feign-client", url = "localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}")
	public interface TestReactiveFeignClient {

		@PostMapping(path = TEST_URL)
		Mono<String> testMethod(TestObject data);

	}

	@ReactiveFeignClient(name = "custom-feign-client", url = "localhost:${"+MOCK_SERVER_PORT_PROPERTY+"}",
			configuration = CustomObjectMapperConfiguration.class)
	public interface TestReactiveFeignClientWithCustomObjectMapper {

		@PostMapping(path = TEST_URL)
		Mono<String> testMethod(TestObject data);

	}


	@EnableReactiveFeignClients(clients = {
			TestReactiveFeignClient.class,
			TestReactiveFeignClientWithCustomObjectMapper.class
	})
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{

		@Bean
		public ObjectMapper objectMapper(){
			return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
		}
	}

	@Configuration
	public static class CustomObjectMapperConfiguration{

		@Bean
		public ObjectMapper objectMapper(){
			return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
		}
	}

	static class TestObject {
		public String fieldWithGoodName;

		public TestObject(String fieldWithGoodName) {
			this.fieldWithGoodName = fieldWithGoodName;
		}
	}
}
