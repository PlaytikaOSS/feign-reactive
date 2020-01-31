package reactivefeign.cloud;

import com.netflix.client.ClientFactory;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.loadbalancer.ILoadBalancer;
import reactivefeign.webclient.WebReactiveFeign;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static reactivefeign.ReactivityTest.CALLS_NUMBER;

public class BuilderUtils {

    private static final AtomicInteger uniqueHystrixCommandCounter = new AtomicInteger();

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(){
        return cloudBuilderWithUniqueHystrixCommand(
                HystrixCommandProperties.Setter()
                        .withExecutionIsolationSemaphoreMaxConcurrentRequests(CALLS_NUMBER), null);
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithExecutionTimeoutDisabled() {
        return cloudBuilderWithUniqueHystrixCommand(
                HystrixCommandProperties.Setter()
                        .withExecutionIsolationSemaphoreMaxConcurrentRequests(CALLS_NUMBER)
                        .withExecutionTimeoutEnabled(false), null);
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithUniqueHystrixCommand(
            HystrixCommandProperties.Setter commandPropertiesDefaults,
            AtomicReference<String> lastCommandKey) {
        int uniqueId = uniqueHystrixCommandCounter.incrementAndGet();
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .setHystrixCommandSetterFactory(
                        (target, methodMetadata) -> {
                            HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(
                                    target.name() +"."+ uniqueId);
                            String circuitBreakerId = methodMetadata.configKey() + "." + uniqueId;
                            if(lastCommandKey != null) {
                                lastCommandKey.set(circuitBreakerId);
                            }
                            return HystrixObservableCommand.Setter
                                    .withGroupKey(groupKey)
                                    .andCommandKey(HystrixCommandKey.Factory.asKey(circuitBreakerId))
                                    .andCommandPropertiesDefaults(commandPropertiesDefaults);
                        }
                );
    }

    public static final ReactiveFeignClientFactory TEST_CLIENT_FACTORY = new ReactiveFeignClientFactory(){

        @Override
        public ILoadBalancer loadBalancer(String name) {
            return ClientFactory.getNamedLoadBalancer(name);
        }

        @Override
        public IClientConfig clientConfig(String name) {
            return DefaultClientConfigImpl.getClientConfigWithDefaultValues(name);
        }
    };

}
