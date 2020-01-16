package reactivefeign.cloud2;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
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
            AtomicReference<HystrixCommandKey> lastCommandKey) {
        int uniqueId = uniqueHystrixCommandCounter.incrementAndGet();
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .setHystrixCommandSetterFactory(
                        (target, methodMetadata) -> {
                            HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(
                                    target.name() +"."+ uniqueId);
                            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(
                                    methodMetadata.configKey() +"."+ uniqueId);
                            if(lastCommandKey != null) {
                                lastCommandKey.set(commandKey);
                            }
                            return HystrixObservableCommand.Setter
                                    .withGroupKey(groupKey)
                                    .andCommandKey(commandKey)
                                    .andCommandPropertiesDefaults(commandPropertiesDefaults);
                        }
                );
    }

}
