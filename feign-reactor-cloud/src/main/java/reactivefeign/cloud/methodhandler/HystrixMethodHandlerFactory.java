package reactivefeign.cloud.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

import static feign.Util.checkNotNull;

public class HystrixMethodHandlerFactory implements MethodHandlerFactory {

    private final MethodHandlerFactory methodHandlerFactory;
    private final CloudReactiveFeign.SetterFactory commandSetterFactory;
    private final Function<Throwable, Object> fallbackFactory;
    private Target target;

    public HystrixMethodHandlerFactory(MethodHandlerFactory methodHandlerFactory,
                                       CloudReactiveFeign.SetterFactory commandSetterFactory,
                                       Function<Throwable, Object> fallbackFactory) {
        this.methodHandlerFactory = checkNotNull(methodHandlerFactory, "methodHandlerFactory must not be null");
        this.commandSetterFactory = checkNotNull(commandSetterFactory, "hystrixObservableCommandSetter must not be null");
        this.fallbackFactory = fallbackFactory;
    }

    @Override
    public void target(Target target) {
        this.target = target;
        methodHandlerFactory.target(target);
    }

    @Override
    public MethodHandler create(final MethodMetadata metadata) {
        return new HystrixMethodHandler(
                target, metadata,
                methodHandlerFactory.create(metadata),
                commandSetterFactory,
                fallbackFactory);
    }

    @Override
    public MethodHandler createDefault(Method method) {
        return methodHandlerFactory.createDefault(method);
    }
}
