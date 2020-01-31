/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.cloud2;

import feign.Target;
import org.junit.BeforeClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.webclient.WebReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class MetricsTest extends reactivefeign.MetricsTest {

  private static String serviceName = "MetricsTest-loadBalancingDefaultPolicyRoundRobin";

    private static ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    @BeforeClass
    public static void setupServersList() {
        loadBalancerFactory = LoadBalancingReactiveHttpClientTest.loadBalancerFactory(serviceName, wireMockRule.port());
    }

  @Override
  protected String getHost() {
    return serviceName;
  }

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder() {
    return BuilderUtils.<IcecreamServiceApi>cloudBuilder()
            .enableLoadBalancer(loadBalancerFactory);
  }

  @Override
  protected Target<IcecreamServiceApi> target(){
    return new Target.HardCodedTarget<>(IcecreamServiceApi.class, serviceName, "http://"+serviceName);
  }

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long readTimeoutInMillis) {
    return BuilderUtils.<IcecreamServiceApi>cloudBuilder()
            .enableLoadBalancer(loadBalancerFactory)
            .options(new WebReactiveOptions.Builder().setReadTimeoutMillis(readTimeoutInMillis).build()
    );
  }

}
