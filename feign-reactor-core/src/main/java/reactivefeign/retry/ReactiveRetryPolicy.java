package reactivefeign.retry;

import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.function.Function;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveRetryPolicy {

    Retry retry();

    interface Builder {
        ReactiveRetryPolicy build();
    }
}
