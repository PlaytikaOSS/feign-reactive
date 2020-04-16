package reactivefeign.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface ReactiveHttpClient<P extends Publisher<?>> {

	Mono<ReactiveHttpResponse<P>> executeRequest(ReactiveHttpRequest request);
}
