package reactivefeign.retry;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
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
    public Function<Flux<Retry.RetrySignal>, Flux<Throwable>> toRetryFunction() {
        return filter(retryPolicy.toRetryFunction(), toRetryOn);
    }

    static Function<Flux<Retry.RetrySignal>, Flux<Throwable>> filter(
            Function<Flux<Retry.RetrySignal>, Flux<Throwable>> retryFunction,
            Predicate<Throwable> toRetryOn){
        return errors -> retryFunction.apply(
                errors.map(throwable -> {
                    if(toRetryOn.test(throwable.failure())){
                        return throwable;
                    } else {
                        throw Exceptions.propagate(throwable.failure());
                    }
                })
        );
    }
}
