package reactivefeign.methodhandler.fallback;

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.methodhandler.MethodHandler;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

import static feign.Util.checkNotNull;
import static reactivefeign.utils.FeignUtils.findMethodInTarget;

/**
 * @author Sergii Karpenko
 */
public class FallbackMethodHandler implements MethodHandler {

    private final Method method;
    private final Type returnPublisherType;
    private final MethodHandler methodHandler;
    private final Function<Throwable, Object> fallbackFactory;

    FallbackMethodHandler(
            Target target, MethodMetadata methodMetadata,
            MethodHandler methodHandler,
            Function<Throwable, Object> fallbackFactory) {
        checkNotNull(target, "target must be not null");

        checkNotNull(methodMetadata, "methodMetadata must be not null");
        method = findMethodInTarget(target, methodMetadata);
        method.setAccessible(true);

        returnPublisherType = ((ParameterizedType) methodMetadata.returnType()).getRawType();
        this.methodHandler = checkNotNull(methodHandler, "methodHandler must be not null");
        this.fallbackFactory = checkNotNull(fallbackFactory, "fallbackFactory must be not null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<Object> invoke(final Object[] argv) {

        Publisher<Object> publisher;
        try {
            publisher = (Publisher) methodHandler.invoke(argv);
        } catch (Throwable throwable) {
            publisher = Mono.error(throwable);
        }

        if(returnPublisherType == Mono.class){
            return ((Mono<Object>)publisher).onErrorResume(throwable -> {
                Object fallback = fallbackFactory.apply(throwable);
                Object fallbackValue = getFallbackValue(fallback, method, argv);
                return (Mono<Object>)fallbackValue;
            });
        } else if(returnPublisherType == Flux.class){
            return ((Flux<Object>)publisher).onErrorResume(throwable -> {
                Object fallback = fallbackFactory.apply(throwable);
                Object fallbackValue = getFallbackValue(fallback, method, argv);
                return (Publisher<Object>)fallbackValue;
            });
        } else {
            throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
        }
    }

    private Object getFallbackValue(Object target, Method method, Object[] argv) {
        try {
            return method.invoke(target, argv);
        } catch (Throwable e) {
            throw Exceptions.propagate(e);
        }
    }
}
