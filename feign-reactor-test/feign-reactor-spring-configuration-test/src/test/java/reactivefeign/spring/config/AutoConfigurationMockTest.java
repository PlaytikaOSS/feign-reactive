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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.spring.config.WebClientCustomizerTest.MOCK_SERVER_PORT_PROPERTY;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AutoConfigurationTest.TestConfiguration.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class AutoConfigurationMockTest {

	private static WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

	@MockBean
	AutoConfigurationTest.TestReactiveFeignClient feignClient;

	@BeforeClass
	public static void setup() {
		mockHttpServer.start();

		System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
	}

	@Test
	public void shouldReplaceFeignClientWithMock() {
		assertThat(feignClient.toString()).isEqualTo("reactivefeign.spring.config.AutoConfigurationTest$TestReactiveFeignClient bean");
	}


}
