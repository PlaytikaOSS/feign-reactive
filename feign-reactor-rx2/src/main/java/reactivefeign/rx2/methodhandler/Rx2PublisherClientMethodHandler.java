package reactivefeign.rx2.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.reactivestreams.Publisher;
import reactivefeign.methodhandler.PublisherClientMethodHandler;
import reactivefeign.publisher.PublisherHttpClient;
import reactor.core.publisher.Mono;

import static reactor.adapter.rxjava.RxJava2Adapter.*;

public class Rx2PublisherClientMethodHandler extends PublisherClientMethodHandler {

    private final BackpressureStrategy backpressureStrategy;

    public Rx2PublisherClientMethodHandler(
            Target target, MethodMetadata methodMetadata,
            PublisherHttpClient publisherClient, BackpressureStrategy backpressureStrategy) {
        super(target, methodMetadata, publisherClient);
        this.backpressureStrategy = backpressureStrategy;
    }

    @Override
    protected Publisher<Object> body(Object body) {
        if (body instanceof Flowable) {
            return flowableToFlux((Flowable<Object>) body);
        } else if (body instanceof Observable) {
            return observableToFlux((Observable<Object>) body,  backpressureStrategy);
        } else if (body instanceof Single) {
            return singleToMono((Single<Object>) body);
        } else if (body instanceof Maybe) {
            return maybeToMono((Maybe<Object>) body);
        } else {
            return Mono.just(body);
        }
    }
}
