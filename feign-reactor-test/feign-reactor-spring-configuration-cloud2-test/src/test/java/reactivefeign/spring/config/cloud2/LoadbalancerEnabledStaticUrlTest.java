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

package reactivefeign.spring.config.cloud2;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.cloud2.LoadBalancerEnabledCircuitBreakerDisabledUsingPropertiesTest.FEIGN_CLIENT_TEST_LB;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LoadbalancerEnabledStaticUrlTest.TestStaticUrlConfiguration.class,
		        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = {
		"classpath:lb-enabled-cb-disabled.properties",
		"classpath:common.properties"
})
@DirtiesContext
public class LoadbalancerEnabledStaticUrlTest {

	static final int MOCK_SERVER_PORT = 9374;

	private static final String TEST_URL = "/testUrl";
	private static final String BODY_TEXT = "test";

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().port(MOCK_SERVER_PORT));

	@Autowired
	TestReactiveFeignClient feignClient;

	@Test
	public void shouldUseStaticUrl() {
		mockHttpServer.stubFor(get(urlPathMatching(TEST_URL))
				.willReturn(aResponse()
						.withBody(BODY_TEXT)
						.withStatus(200)));

		Mono<String> result = feignClient.testMethod();

		StepVerifier.create(result)
				.expectNext(BODY_TEXT)
				.verifyComplete();

		assertThat(mockHttpServer.getAllServeEvents().size()).isEqualTo(1);
	}

	@BeforeClass
	public static void setup() {
		mockHttpServer.start();
	}

	@AfterClass
	public static void teardown() {
		mockHttpServer.stop();
	}

	@Before
	public void reset(){
		mockHttpServer.resetAll();
	}

	@ReactiveFeignClient(name = FEIGN_CLIENT_TEST_LB, url = "localhost:"+MOCK_SERVER_PORT)
	public interface TestReactiveFeignClient {

		@GetMapping(path = TEST_URL)
		Mono<String> testMethod();

	}

	@EnableReactiveFeignClients(clients = LoadbalancerEnabledStaticUrlTest.TestReactiveFeignClient.class)
	@EnableAutoConfiguration
	@Configuration
	public static class TestStaticUrlConfiguration {}
}
