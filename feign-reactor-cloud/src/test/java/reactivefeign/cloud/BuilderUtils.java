package reactivefeign.cloud;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactivefeign.webclient.WebReactiveFeign;

import static reactivefeign.ReactivityTest.CALLS_NUMBER;

public class BuilderUtils {

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(){
        return cloudBuilder(null);
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(String hystrixGroupPrefix){
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .setHystrixCommandSetterFactory(
                        (target, methodMetadata) -> {
                            String groupKey = hystrixGroupPrefix+"."+target.name();
                            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(methodMetadata.configKey());
                            return HystrixObservableCommand.Setter
                                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                                    .andCommandKey(commandKey)
                                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                            .withExecutionIsolationSemaphoreMaxConcurrentRequests(CALLS_NUMBER)
                                    );
                        }
                );
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithExecutionTimeoutDisabled(String hystrixGroupPrefix) {
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .setHystrixCommandSetterFactory(
                (target, methodMetadata) -> {
                    String groupKey = hystrixGroupPrefix + target.name();
                    HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(methodMetadata.configKey());
                    return HystrixObservableCommand.Setter
                            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                            .andCommandKey(commandKey)
                            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                    .withExecutionIsolationSemaphoreMaxConcurrentRequests(CALLS_NUMBER)
                                    .withExecutionTimeoutEnabled(false)
                            );
                }
        );
    }

}
