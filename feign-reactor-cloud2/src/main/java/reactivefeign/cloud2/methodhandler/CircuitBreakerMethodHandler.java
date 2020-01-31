package reactivefeign.cloud2.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.lang.Nullable;
import reactivefeign.cloud2.ReactiveFeignCircuitBreakerFactory;
import reactivefeign.methodhandler.MethodHandler;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

import static feign.Util.checkNotNull;
import static reactivefeign.utils.FeignUtils.findMethodInTarget;

/**
 * @author Sergii Karpenko
 */
public class CircuitBreakerMethodHandler implements MethodHandler {

    private final MethodHandler methodHandler;
    private final BiFunction<Throwable, Object[], Object> fallbackFactory;
    private final ReactiveCircuitBreaker reactiveCircuitBreaker;

    CircuitBreakerMethodHandler(
            Target target, MethodMetadata methodMetadata,
            MethodHandler methodHandler,
            ReactiveFeignCircuitBreakerFactory reactiveCircuitBreakerFactory,
            @Nullable Function<Throwable, Object> fallbackInstanceFactory) {
        checkNotNull(target, "target must be not null");

        checkNotNull(methodMetadata, "methodMetadata must be not null");

        this.methodHandler = checkNotNull(methodHandler, "methodHandler must be not null");
        checkNotNull(reactiveCircuitBreakerFactory, "reactiveCircuitBreakerFactory must be not null");
        String circuitBreakerId = methodMetadata.configKey();
        this.reactiveCircuitBreaker = reactiveCircuitBreakerFactory.apply(circuitBreakerId);

        Method method = findMethodInTarget(target, methodMetadata);
        method.setAccessible(true);
        this.fallbackFactory = buildFallbackFactory(fallbackInstanceFactory, method);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<Object> invoke(final Object[] argv) throws Throwable {
        Object publisher = methodHandler.invoke(argv);
        if(publisher instanceof Mono){
            if(fallbackFactory != null){
                return reactiveCircuitBreaker.run((Mono) publisher,
                        t -> (Mono)fallbackFactory.apply(t, argv));
            } else {
                return reactiveCircuitBreaker.run((Mono) publisher);
            }

        } else {
            if(fallbackFactory != null){
                return reactiveCircuitBreaker.run((Flux) publisher,
                        t -> (Flux)fallbackFactory.apply(t, argv));
            } else {
                return reactiveCircuitBreaker.run((Flux) publisher);
            }
        }
    }

    protected BiFunction<Throwable, Object[], Object> buildFallbackFactory(
            Function<Throwable, Object> fallbackInstanceFactory, Method method) {
        if(fallbackInstanceFactory == null){
            return null;
        }
        return (throwable, argv) -> {
            Object fallbackInstance = fallbackInstanceFactory.apply(throwable);
            try {
                return method.invoke(fallbackInstance, argv);
            }
            catch (InvocationTargetException e){
                throw Exceptions.propagate(e.getCause());
            }
            catch (Throwable e) {
                throw Exceptions.propagate(e);
            }
        };
    }
}
