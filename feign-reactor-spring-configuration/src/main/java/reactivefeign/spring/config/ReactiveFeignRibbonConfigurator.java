/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package reactivefeign.spring.config;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.cloud.LoadBalancerCommandFactory;

public class ReactiveFeignRibbonConfigurator extends AbstractReactiveFeignConfigurator{

    protected ReactiveFeignRibbonConfigurator() {
        super(2);
    }

    @Override
    public ReactiveFeignBuilder configure(
            ReactiveFeignBuilder builder,
            ReactiveFeignNamedContext namedContext) {
        if (!(builder instanceof CloudReactiveFeign.Builder)) {
            throw new IllegalArgumentException("CloudReactiveFeign.Builder expected");
        }

        CloudReactiveFeign.Builder cloudBuilder = (CloudReactiveFeign.Builder) builder;

        String clientName = namedContext.getClientName();
        LoadBalancerCommandFactory balancerCommandFactory = namedContext.getOptional(LoadBalancerCommandFactory.class);
        if(balancerCommandFactory == null){

            SpringClientFactory springClientFactory = namedContext.getOptional(SpringClientFactory.class);

            balancerCommandFactory = serviceName -> {

                IClientConfig clientConfig = springClientFactory.getClientConfig(clientName);
                ILoadBalancer namedLoadBalancer = springClientFactory.getLoadBalancer(clientName);
                RetryHandler retryHandler = getOrInstantiateRetryHandler(namedContext, clientConfig);

                return LoadBalancerCommand.builder()
                        .withLoadBalancer(namedLoadBalancer)
                        .withRetryHandler(retryHandler)
                        .withClientConfig(clientConfig)
                        .build();
            };
        }

        cloudBuilder = cloudBuilder.setLoadBalancerCommandFactory(balancerCommandFactory);

        return cloudBuilder;
    }

    private RetryHandler getOrInstantiateRetryHandler(ReactiveFeignNamedContext namedContext, IClientConfig clientConfig) {
        RetryHandler retryHandler = namedContext.getOptional(RetryHandler.class);
        if(retryHandler == null){
            retryHandler = new DefaultLoadBalancerRetryHandler(clientConfig);
        }
        return retryHandler;
    }
}
