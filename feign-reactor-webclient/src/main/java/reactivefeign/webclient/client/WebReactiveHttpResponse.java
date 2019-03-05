package reactivefeign.webclient.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

class WebReactiveHttpResponse implements ReactiveHttpResponse{

	private ReactiveHttpRequest reactiveRequest;
	private final ClientResponse clientResponse;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference<Object> returnActualType;

	WebReactiveHttpResponse(ReactiveHttpRequest reactiveRequest,
							ClientResponse clientResponse,
							Type returnPublisherType, ParameterizedTypeReference<Object> returnActualType) {
		this.reactiveRequest = reactiveRequest;
		this.clientResponse = clientResponse;
		this.returnPublisherType = returnPublisherType;
		this.returnActualType = returnActualType;
	}

	@Override
	public ReactiveHttpRequest request() {
		return reactiveRequest;
	}

	@Override
	public int status() {
		return clientResponse.statusCode().value();
	}

	@Override
	public Map<String, List<String>> headers() {
		return clientResponse.headers().asHttpHeaders();
	}

	@Override
	public Publisher<Object> body() {
		if (returnPublisherType == Mono.class) {
			return clientResponse.bodyToMono(returnActualType);
		} else if(returnPublisherType == Flux.class){
			return clientResponse.bodyToFlux(returnActualType);
		} else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
		}
	}

	@Override
	public Mono<byte[]> bodyData() {
		return clientResponse.bodyToMono(ByteArrayResource.class)
				.map(ByteArrayResource::getByteArray)
				.defaultIfEmpty(new byte[0]);
	}
}
