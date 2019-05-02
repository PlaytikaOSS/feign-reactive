package reactivefeign.publisher;

import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;

/**
 * @author Sergii Karpenko
 */
public interface PublisherHttpClient {

	Publisher<Object> executeRequest(ReactiveHttpRequest request);
}
