package reactivefeign.cloud;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;

public class BuilderUtils {

    static ReactiveFeign.Builder<IcecreamServiceApi> builderWithExecutionTimeoutDisabled() {
        return CloudReactiveFeign.<IcecreamServiceApi>builder().setHystrixCommandSetterFactory(
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
