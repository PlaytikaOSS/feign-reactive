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

package reactivefeign.cloud;

import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactivefeign.allfeatures.AllFeaturesController;
import reactivefeign.allfeatures.AllFeaturesFeign;
import reactivefeign.webclient.WebReactiveFeign;

import static java.util.Arrays.asList;
import static reactivefeign.ReactivityTest.CALLS_NUMBER;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */
@SpringBootTest(
		properties = {"spring.main.web-application-type=reactive"},
		classes = {AllFeaturesController.class, reactivefeign.allfeatures.AllFeaturesTest.TestConfiguration.class },
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
public class AllFeaturesTest extends reactivefeign.allfeatures.AllFeaturesTest {

	private static final String serviceName = "testServiceName";

	@BeforeClass
	public static void setupServersList() throws ClientException {
		DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
		clientConfig.loadDefaultValues();
		clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerClassName, BaseLoadBalancer.class.getName());
		ILoadBalancer lb = ClientFactory.registerNamedLoadBalancerFromclientConfig(serviceName, clientConfig);
		lb.addServers(asList(new Server("localhost", 8080)));
	}

	@Override
	protected AllFeaturesApi buildClient() {
		return BuilderUtils.<AllFeaturesFeign>cloudBuilder("AllFeaturesTest")
				.enableLoadBalancer()
				.decode404()
				.target(AllFeaturesFeign.class, "http://"+serviceName);
	}

	@Override
	protected AllFeaturesApi buildClient(String url) {
		throw new UnsupportedOperationException();
	}

	@Test(expected = HystrixRuntimeException.class)
	public void shouldFailIfNoSubstitutionForPath(){
		super.shouldFailIfNoSubstitutionForPath();
	}

	//Netty's WebClient is not able to do this trick
	@Ignore
	@Test
	@Override
	public void shouldReturnFirstResultBeforeSecondSent() {
	}

	//WebClient is not able to do this
	@Ignore
	@Test
	@Override
	public void shouldMirrorStringStreamBody() {
	}
}
