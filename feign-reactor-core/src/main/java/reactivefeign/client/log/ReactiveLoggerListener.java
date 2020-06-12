package reactivefeign.client.log;

import feign.MethodMetadata;
import feign.Target;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;

/**
 * May be used to collect request execution metrics as well as to just log
 *
 * @author Sergii Karpenko
 */
public interface ReactiveLoggerListener<CONTEXT> {

    CONTEXT requestStarted(ReactiveHttpRequest request, Target<?> target, MethodMetadata methodMetadata);

    boolean logRequestBody();

    void bodySent(Object body, CONTEXT context);

    void responseReceived(ReactiveHttpResponse<?> response, CONTEXT context);

    void errorReceived(Throwable throwable, CONTEXT context);

    boolean logResponseBody();

    void bodyReceived(Object body, CONTEXT context);
}
