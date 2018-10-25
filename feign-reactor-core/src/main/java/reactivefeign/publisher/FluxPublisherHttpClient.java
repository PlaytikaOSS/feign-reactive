package reactivefeign.publisher;


import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Wraps {@link PublisherHttpClient}
 *
 * @author Sergii Karpenko
 */
public class FluxPublisherHttpClient implements PublisherHttpClient {

	private final ReactiveHttpClient reactiveHttpClient;

	public FluxPublisherHttpClient(ReactiveHttpClient reactiveHttpClient) {
		this.reactiveHttpClient = reactiveHttpClient;
	}

	@Override
	public Flux<?> executeRequest(ReactiveHttpRequest request) {
		Mono<ReactiveHttpResponse> response = reactiveHttpClient.executeRequest(request);
		return response.flatMapMany(ReactiveHttpResponse::body);
	}
}
