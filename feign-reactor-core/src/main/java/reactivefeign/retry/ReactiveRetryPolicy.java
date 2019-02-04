package reactivefeign.retry;

import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveRetryPolicy {

    Function<Flux<Throwable>, Flux<Throwable>> toRetryFunction();

    interface Builder {
        ReactiveRetryPolicy build();
    }
}
