package reactivefeign.publisher;

import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;

import java.lang.reflect.Type;

/**
 * @author Sergii Karpenko
 */
public interface PublisherHttpClient {

	Publisher<?> executeRequest(ReactiveHttpRequest request);
}
