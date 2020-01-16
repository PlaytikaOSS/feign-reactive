package reactivefeign.publisher.retry;

import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpRequest;

public class OutOfRetriesException extends ReactiveFeignException {

    OutOfRetriesException(Throwable cause, ReactiveHttpRequest request) {
        super(cause, request);
    }
}
