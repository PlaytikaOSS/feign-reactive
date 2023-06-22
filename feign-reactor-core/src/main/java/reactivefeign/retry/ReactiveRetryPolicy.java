package reactivefeign.retry;

import feign.ExceptionPropagationPolicy;
import reactor.util.retry.Retry;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveRetryPolicy {

    Retry retry();

    int maxAllowedRetries();

    interface Builder {
        ReactiveRetryPolicy build();
    }

    default ExceptionPropagationPolicy exceptionPropagationPolicy(){
        return ExceptionPropagationPolicy.NONE;
    }
}
