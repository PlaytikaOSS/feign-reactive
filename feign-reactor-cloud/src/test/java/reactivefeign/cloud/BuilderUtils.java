package reactivefeign.cloud;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactivefeign.webclient.WebReactiveFeign;

public class BuilderUtils {

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(){
        return CloudReactiveFeign.builder(WebReactiveFeign.builder());
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithExecutionTimeoutDisabled() {
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .setHystrixCommandSetterFactory(
                (target, methodMetadata) -> {
                    String groupKey = target.name();
                    HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(methodMetadata.configKey());
                    return HystrixObservableCommand.Setter
                            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                            .andCommandKey(commandKey)
                            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                    .withExecutionTimeoutEnabled(false)
                            );
                }
        );
    }

}
