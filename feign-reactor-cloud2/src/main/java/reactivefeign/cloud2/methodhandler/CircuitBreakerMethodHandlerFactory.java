package reactivefeign.cloud2.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import org.springframework.lang.Nullable;
import reactivefeign.cloud2.ReactiveFeignCircuitBreakerFactory;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

import static feign.Util.checkNotNull;

public class CircuitBreakerMethodHandlerFactory implements MethodHandlerFactory {

    private final MethodHandlerFactory methodHandlerFactory;
    private final ReactiveFeignCircuitBreakerFactory reactiveFeignCircuitBreakerFactory;
    private final Function<Throwable, Object> fallbackFactory;
    private Target target;

    public CircuitBreakerMethodHandlerFactory(MethodHandlerFactory methodHandlerFactory,
                                              ReactiveFeignCircuitBreakerFactory reactiveFeignCircuitBreakerFactory,
                                              @Nullable Function<Throwable, Object> fallbackFactory) {
        this.methodHandlerFactory = checkNotNull(methodHandlerFactory, "methodHandlerFactory must not be null");
        this.reactiveFeignCircuitBreakerFactory = checkNotNull(reactiveFeignCircuitBreakerFactory, "reactiveFeignCircuitBreakerFactory must not be null");
        this.fallbackFactory = fallbackFactory;
    }

    @Override
    public void target(Target target) {
        this.target = target;
        methodHandlerFactory.target(target);
    }

    @Override
    public MethodHandler create(final MethodMetadata metadata) {
        return new CircuitBreakerMethodHandler(
                target, metadata,
                methodHandlerFactory.create(metadata),
                reactiveFeignCircuitBreakerFactory,
                fallbackFactory);
    }

    @Override
    public MethodHandler createDefault(Method method) {
        return methodHandlerFactory.createDefault(method);
    }
}
