package reactivefeign.webclient.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
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

	final ReactiveHttpRequest reactiveRequest;
	final ClientResponse clientResponse;
	final Type returnPublisherType;
	final ParameterizedTypeReference<?> returnActualType;


	WebReactiveHttpResponse(ReactiveHttpRequest reactiveRequest,
							ClientResponse clientResponse,
							Type returnPublisherType, ParameterizedTypeReference<?> returnActualType) {
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
	public Mono<Void> releaseBody() {
		return clientResponse.releaseBody();
	}

	@Override
	public Mono<byte[]> bodyData() {
		Flux<DataBuffer> response = clientResponse.body(BodyExtractors.toDataBuffers());
		return DataBufferUtils.join(response)
				.map(WebReactiveHttpResponse::readToByteArray)
				.defaultIfEmpty(new byte[0]);
	}

	private static byte[] readToByteArray(DataBuffer dataBuffer) {
		byte[] result = new byte[dataBuffer.readableByteCount()];
		dataBuffer.read(result);
		DataBufferUtils.release(dataBuffer);
		return result;
	}
}
