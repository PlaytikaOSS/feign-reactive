package reactivefeign.client.log;

import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;

/**
 * May be used to collect request execution metrics as well as to just log
 *
 * @author Sergii Karpenko
 */
public interface ReactiveLoggerListener<CONTEXT> {

    CONTEXT requestStarted(ReactiveHttpRequest request, String methodTag);

    boolean logRequestBody();

    void bodySent(ReactiveHttpRequest request, String methodTag, CONTEXT context, Object body);

    void responseReceived(ReactiveHttpResponse response, String methodTag, CONTEXT context);

    boolean logResponseBody();

    void bodyReceived(ReactiveHttpResponse response, String methodTag, CONTEXT context, Object body);
}
