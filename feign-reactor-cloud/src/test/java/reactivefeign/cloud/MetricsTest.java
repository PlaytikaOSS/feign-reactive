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
package reactivefeign.cloud;

import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Target;
import org.junit.BeforeClass;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.webclient.WebReactiveOptions;

import java.util.Collections;

import static reactivefeign.cloud.AllFeaturesTest.setupServersList;
import static reactivefeign.cloud.BuilderUtils.TEST_CLIENT_FACTORY;

/**
 * @author Sergii Karpenko
 */
public class MetricsTest extends reactivefeign.MetricsTest {

  private static String serviceName = "MetricsTest-loadBalancingDefaultPolicyRoundRobin";

  @BeforeClass
  public static void setUpServersList() throws ClientException {
    setupServersList(serviceName, wireMockRule.port());
  }

  @Override
  protected String getHost() {
    return serviceName;
  }

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder() {
    return BuilderUtils.<IcecreamServiceApi>cloudBuilderWithExecutionTimeoutDisabled()
            .enableLoadBalancer(TEST_CLIENT_FACTORY);
  }

  @Override
  protected Target<IcecreamServiceApi> target(){
    return new Target.HardCodedTarget<>(IcecreamServiceApi.class, serviceName, "http://"+serviceName);
  }

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long readTimeoutInMillis) {
    return BuilderUtils.<IcecreamServiceApi>cloudBuilderWithExecutionTimeoutDisabled()
            .enableLoadBalancer(TEST_CLIENT_FACTORY)
            .options(new WebReactiveOptions.Builder().setReadTimeoutMillis(readTimeoutInMillis).build()
    );
  }

}
