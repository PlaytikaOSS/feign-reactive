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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.netflix.client.ClientException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.RibbonEnabledHystrixDisabledUsingPropertiesTest.MOCK_SERVER_1_PORT_PROPERTY;
import static reactivefeign.spring.config.RibbonEnabledHystrixDisabledUsingPropertiesTest.MOCK_SERVER_2_PORT_PROPERTY;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RibbonEnabledHystrixDisabledUsingPropertiesTest.TestConfiguration.class,
		        webEnvironment = SpringBootTest.WebEnvironment.NONE,
		        properties = "ribbon.listOfServers=localhost:${"+MOCK_SERVER_1_PORT_PROPERTY+"}, localhost:${"+MOCK_SERVER_2_PORT_PROPERTY+"}")
@TestPropertySource("classpath:ribbon-enabled-hystrix-disabled.properties")
@DirtiesContext
public class RibbonEnabledHystrixDisabledUsingPropertiesTest {

	static final String MOCK_SERVER_1_PORT_PROPERTY = "mock.server1.port";
	static final String MOCK_SERVER_2_PORT_PROPERTY = "mock.server2.port";

	static final String FEIGN_CLIENT_TEST_RIBBON = "feign-client-test-ribbon";
	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";

	private static WireMockServer mockHttpServer1 = new WireMockServer(wireMockConfig().dynamicPort());
	private static WireMockServer mockHttpServer2 = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldRetryAndNotFailOnDefaultHystrixTimeout() {
		Stream.of(mockHttpServer1, mockHttpServer2).forEach(wireMockServer -> {
			wireMockServer.stubFor(get(urlPathMatching(TEST_URL))
					.willReturn(aResponse()
							.withFixedDelay(700)
							.withBody(BODY_TEXT)
							.withStatus(200)));
		});

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectError(ClientException.class)
				.verify();

		assertThat(mockHttpServer1.getAllServeEvents().size()).isEqualTo(1);
		assertThat(mockHttpServer2.getAllServeEvents().size()).isEqualTo(1);
	}

	@BeforeClass
	public static void setup() {
		mockHttpServer1.start();
		System.setProperty(MOCK_SERVER_1_PORT_PROPERTY, Integer.toString(mockHttpServer1.port()));

		mockHttpServer2.start();
		System.setProperty(MOCK_SERVER_2_PORT_PROPERTY, Integer.toString(mockHttpServer2.port()));
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer1.stop();
		mockHttpServer2.stop();
	}

	@Before
	public void reset(){
		mockHttpServer1.resetAll();
		mockHttpServer2.resetAll();
	}

	@ReactiveFeignClient(name = FEIGN_CLIENT_TEST_RIBBON)
	public interface TestReactiveFeignClient {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

	}

	@EnableReactiveFeignClients(clients = RibbonEnabledHystrixDisabledUsingPropertiesTest.TestReactiveFeignClient.class)
	@EnableAutoConfiguration
	@Configuration
	public static class TestConfiguration{}
}
