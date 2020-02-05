package reactivefeign.cloud.methodhandler;

import com.netflix.hystrix.HystrixObservableCommand;
import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.cloud.CloudReactiveFeign;
import reactivefeign.methodhandler.MethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import rx.Observable;
import rx.RxReactiveStreams;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static feign.Util.checkNotNull;
import static reactivefeign.cloud.SubscriberContextUtils.withContext;
import static reactivefeign.utils.FeignUtils.findMethodInTarget;

/**
 * @author Sergii Karpenko
 */
public class HystrixMethodHandler implements MethodHandler {

    private final Method method;
    private final Type returnPublisherType;
    private final MethodHandler methodHandler;
    private final Function<Throwable, Object> fallbackFactory;
    private final HystrixObservableCommand.Setter hystrixObservableCommandSetter;

    HystrixMethodHandler(
            Target target, MethodMetadata methodMetadata,
            MethodHandler methodHandler,
            CloudReactiveFeign.SetterFactory setterFactory,
            Function<Throwable, Object> fallbackFactory) {
        checkNotNull(target, "target must be not null");

        checkNotNull(methodMetadata, "methodMetadata must be not null");
        method = findMethodInTarget(target, methodMetadata);
        method.setAccessible(true);

        returnPublisherType = ((ParameterizedType) methodMetadata.returnType()).getRawType();
        this.methodHandler = checkNotNull(methodHandler, "methodHandler must be not null");
        this.fallbackFactory = fallbackFactory;
        checkNotNull(setterFactory, "setterFactory must be not null");
        hystrixObservableCommandSetter = setterFactory.create(target, methodMetadata);
    }

    @Override
    public Publisher<Object> invoke(final Object[] argv) {
        if(returnPublisherType == Mono.class) {
            return Mono.subscriberContext()
                    .flatMap(context -> Mono.from(RxReactiveStreams.toPublisher(getHystrixObservableCommand(context, argv).toSingle())))
                    .onErrorResume(
                            throwable -> throwable instanceof NoSuchElementException
                                         && throwable.getMessage().equals("Observable emitted no items"),
                            throwable -> Mono.empty());
        } else if(returnPublisherType == Flux.class) {
            return Mono.subscriberContext()
                    .flatMapMany(context -> Flux.from(RxReactiveStreams.toPublisher(getHystrixObservableCommand(context, argv))));
        } else {
            throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
        }
    }

    protected Object getFallbackValue(Object target, Method method, Object[] argv) throws Throwable {
        return method.invoke(target, argv);
    }

    @SuppressWarnings("unchecked")
    private Observable<Object> getHystrixObservableCommand(final Context context, final Object[] argv) {
        return new HystrixObservableCommand<Object>(hystrixObservableCommandSetter) {

            @Override
            protected Observable<Object> construct() {
                try {
                    Publisher publisher = (Publisher) methodHandler.invoke(argv);
                    return RxReactiveStreams.toObservable(withContext(publisher, returnPublisherType, context));
                } catch (Throwable throwable) {
                    return Observable.error(throwable);
                }
            }

            @Override
            protected Observable<Object> resumeWithFallback() {
                if (fallbackFactory != null) {
                    Object fallback = fallbackFactory.apply(getExecutionException());
                    try {
                        Publisher fallbackValue = ((Publisher) getFallbackValue(fallback, method, argv));
                        return RxReactiveStreams.toObservable(withContext(fallbackValue, returnPublisherType, context));
                    } catch (Throwable throwable) {
                        return Observable.error(throwable);
                    }
                } else {
                    return super.resumeWithFallback();
                }
            }

        }.toObservable();
    }

}
