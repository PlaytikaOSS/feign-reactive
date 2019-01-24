package reactivefeign.methodhandler.fallback;

import feign.MethodMetadata;
import feign.Target;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

import static feign.Util.checkNotNull;

public class FallbackMethodHandlerFactory implements MethodHandlerFactory {

    private final MethodHandlerFactory methodHandlerFactory;
    private final Function<Throwable, Object> fallbackFactory;
    private Target target;

    public FallbackMethodHandlerFactory(MethodHandlerFactory methodHandlerFactory,
                                        Function<Throwable, Object> fallbackFactory) {
        this.methodHandlerFactory = checkNotNull(methodHandlerFactory, "methodHandlerFactory must not be null");
        this.fallbackFactory = checkNotNull(fallbackFactory, "fallbackFactory must be not null");;
    }

    @Override
    public void target(Target target) {
        this.target = target;
        methodHandlerFactory.target(target);
    }

    @Override
    public MethodHandler create(final MethodMetadata metadata) {
        return new FallbackMethodHandler(
                target, metadata,
                methodHandlerFactory.create(metadata),
                fallbackFactory);
    }

    @Override
    public MethodHandler createDefault(Method method) {
        return methodHandlerFactory.createDefault(method);
    }
}
