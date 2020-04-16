package reactivefeign.publisher;


import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactor.core.publisher.Mono;

/**
 * Wraps {@link PublisherHttpClient}
 *
 * @author Sergii Karpenko
 */
public class ResponsePublisherHttpClient implements PublisherHttpClient {

	private final ReactiveHttpClient reactiveHttpClient;

	public ResponsePublisherHttpClient(ReactiveHttpClient reactiveHttpClient) {
		this.reactiveHttpClient = reactiveHttpClient;
	}

	@Override
	public Mono<Object> executeRequest(ReactiveHttpRequest request) {
		return Mono.defer(() -> reactiveHttpClient.executeRequest(request));
	}
}
