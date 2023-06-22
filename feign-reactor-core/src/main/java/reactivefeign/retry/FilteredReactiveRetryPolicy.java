package reactivefeign.retry;

import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FilteredReactiveRetryPolicy implements ReactiveRetryPolicy {

    private final ReactiveRetryPolicy retryPolicy;
    private final Predicate<Throwable> toRetryOn;

    public static FilteredReactiveRetryPolicy notRetryOn(ReactiveRetryPolicy retryPolicy, Class<? extends Throwable>... errorClasses) {
        return new FilteredReactiveRetryPolicy(retryPolicy,
                throwable -> Stream.of(errorClasses)
                        .noneMatch(errorClass -> errorClass.isAssignableFrom(throwable.getClass())));
    }

    public FilteredReactiveRetryPolicy(ReactiveRetryPolicy retryPolicy, Predicate<Throwable> toRetryOn) {
        this.retryPolicy = retryPolicy;
        this.toRetryOn = toRetryOn;
    }

    @Override
    public Retry retry() {
        return filter(retryPolicy.retry(), toRetryOn);
    }

    @Override
    public int maxAllowedRetries() {
        return retryPolicy.maxAllowedRetries();
    }

    static Retry filter(
            Retry retry,
            Predicate<Throwable> toRetryOn){
        return new Retry(){
            @Override
            public Publisher<?> generateCompanion(Flux<RetrySignal> retrySignals) {
                return retry.generateCompanion(retrySignals.map(retrySignal -> {
                    if (toRetryOn.test(retrySignal.failure())) {
                        return retrySignal;
                    } else {
                        throw Exceptions.propagate(retrySignal.failure());
                    }
                }));
            }
        };
    }
}
