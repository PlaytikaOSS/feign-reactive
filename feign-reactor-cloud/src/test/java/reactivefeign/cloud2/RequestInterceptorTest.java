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

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Before;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.testcase.IcecreamServiceApi;

import static reactivefeign.cloud2.LoadBalancingReactiveHttpClientTest.loadBalancerFactory;

/**
 * @author Sergii Karpenko
 */
public class RequestInterceptorTest extends reactivefeign.RequestInterceptorTest {

    protected static String serviceName = "RequestInterceptorTest";

    private ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory
            = new ReactiveResilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults()
            );

    private ReactiveLoadBalancer.Factory<ServiceInstance> lbFactory;

    @Before
    public void setupServersList() {
        lbFactory = loadBalancerFactory(serviceName, wireMockRule.port());
    }

    @Override
    protected ReactiveFeignBuilder<IcecreamServiceApi> builder() {
        return reactivefeign.cloud2.BuilderUtils.<IcecreamServiceApi>
                cloudBuilderWithExecutionTimeoutDisabled(circuitBreakerFactory, null)
                .enableLoadBalancer(lbFactory);
    }

    @Override
    protected IcecreamServiceApi target(ReactiveFeignBuilder<IcecreamServiceApi> builder){
        return builder.target(IcecreamServiceApi.class, serviceName, "http://" + serviceName);
    }

}
