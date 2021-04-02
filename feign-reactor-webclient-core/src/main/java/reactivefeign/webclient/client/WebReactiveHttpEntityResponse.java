package reactivefeign.webclient.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactivefeign.client.ReactiveHttpRequest;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

class WebReactiveHttpEntityResponse<P extends Publisher<?>> extends WebReactiveHttpResponse<P>{


	WebReactiveHttpEntityResponse(ReactiveHttpRequest reactiveRequest, ClientResponse clientResponse, Type returnPublisherType, ParameterizedTypeReference<?> returnActualType) {
		super(reactiveRequest, clientResponse, returnPublisherType, returnActualType);
	}

	@Override
	public P body() {
		return (P)Mono.just(new ResponseEntity<>(
				super.body(),
				clientResponse.headers().asHttpHeaders(),
				clientResponse.statusCode()));
	}
}
