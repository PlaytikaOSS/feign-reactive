package reactivefeign.cloud2;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactivefeign.webclient.WebReactiveFeign;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BuilderUtils {

    private static final AtomicInteger uniqueCircuitBreakerCounter = new AtomicInteger();

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(){
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder());
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilder(ReactiveCircuitBreakerFactory circuitBreakerFactory){
        return cloudBuilderWithUniqueCircuitBreaker(
                circuitBreakerFactory, null, null);
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithExecutionTimeoutDisabled(
            ReactiveCircuitBreakerFactory circuitBreakerFactory,
            AtomicReference<String> lastCircuitBreakerKey) {
        return cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder).timeLimiterConfig(
                        TimeLimiterConfig.custom().timeoutDuration(Duration.ofMinutes(10)).build()),
                lastCircuitBreakerKey);
    }

    static <T> CloudReactiveFeign.Builder<T> cloudBuilderWithUniqueCircuitBreaker(
            ReactiveCircuitBreakerFactory circuitBreakerFactory,
            Consumer<ConfigBuilder> customizer,
            AtomicReference<String> lastCircuitBreakerId) {
        int uniqueId = uniqueCircuitBreakerCounter.incrementAndGet();
        return CloudReactiveFeign.<T>builder(WebReactiveFeign.builder())
                .enableCircuitBreaker(circuitBreakerId -> {
                    String uniqueCircuitBreakerId = circuitBreakerId + "."+uniqueId;
                    if(lastCircuitBreakerId != null) {
                        lastCircuitBreakerId.set(uniqueCircuitBreakerId);
                    }
                    if(customizer != null) {
                        circuitBreakerFactory.configure(customizer, uniqueCircuitBreakerId);
                    }
                    return circuitBreakerFactory.create(uniqueCircuitBreakerId);
                });
    }

}
