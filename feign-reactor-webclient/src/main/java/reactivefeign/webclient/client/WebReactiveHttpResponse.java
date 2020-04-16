package reactivefeign.webclient.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

class WebReactiveHttpResponse<P extends Publisher<?>> implements ReactiveHttpResponse<P>{

	private ReactiveHttpRequest reactiveRequest;
	private final ClientResponse clientResponse;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference returnActualType;

	private final ByteArrayDecoder byteArrayDecoder = new ByteArrayDecoder();

	WebReactiveHttpResponse(ReactiveHttpRequest reactiveRequest,
							ClientResponse clientResponse,
							Type returnPublisherType, ParameterizedTypeReference returnActualType) {
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
	public P body() {
		if (returnPublisherType == Mono.class) {
			return (P)clientResponse.bodyToMono(returnActualType);
		} else if(returnPublisherType == Flux.class){
			return (P)clientResponse.bodyToFlux(returnActualType);
		} else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
		}
	}

	@Override
	public Mono<byte[]> bodyData() {
		Flux<DataBuffer> response = clientResponse.body(BodyExtractors.toDataBuffers());
		return DataBufferUtils.join(response)
				.map(dataBuffer -> byteArrayDecoder.decode(dataBuffer, ResolvableType.NONE, null, null))
				.defaultIfEmpty(new byte[0]);
	}
}
