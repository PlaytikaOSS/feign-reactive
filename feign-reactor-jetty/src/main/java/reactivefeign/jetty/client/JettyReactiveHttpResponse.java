package reactivefeign.jetty.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivejson.ReactorObjectReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;

class JettyReactiveHttpResponse implements ReactiveHttpResponse{

	public static final String CHARSET_DELIMITER = ";charset=";
	private ReactiveHttpRequest request;
	private final Response clientResponse;
	private final Publisher<ContentChunk> contentChunks;
	private final Class returnPublisherType;
	private Class<?> returnActualClass;
	private final ObjectReader objectReader;
	private final JsonFactory jsonFactory;

	JettyReactiveHttpResponse(ReactiveHttpRequest request, Response clientResponse, Publisher<ContentChunk> contentChunks,
							  Class returnPublisherType, Class returnActualClass,
							  JsonFactory jsonFactory, ObjectReader objectReader) {
		this.request = request;
		this.clientResponse = clientResponse;
		this.contentChunks = contentChunks;
		this.returnPublisherType = returnPublisherType;
		this.returnActualClass = returnActualClass;
		this.objectReader = objectReader;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public ReactiveHttpRequest request() {
		return request;
	}

	@Override
	public int status() {
		return clientResponse.getStatus();
	}

	@Override
	public Map<String, List<String>> headers() {
		HttpFields headers = clientResponse.getHeaders();
		Map<String, List<String>> headersMap = new HashMap<>(headers.size());
		headers.forEach(httpField ->
				headersMap.compute(httpField.getName(), (oldName, oldValues) -> {
					List<String> values;
					if(oldValues == null){
						values = Arrays.asList(httpField.getValues());
					} else {
						values = new ArrayList<>(oldValues.size() + httpField.getValues().length);
						values.addAll(oldValues);
						values.addAll(Arrays.asList(httpField.getValues()));
					}
					return values;
				}));
		return headersMap;
	}

	@Override
	public Publisher<?> body() {
		ReactorObjectReader reactorObjectReader = new ReactorObjectReader(jsonFactory);

		Flux<ByteBuffer> content = directContent();

		if(returnActualClass == ByteBuffer.class){
			return content;
		} else if(returnActualClass.isAssignableFrom(String.class)
				&& returnPublisherType == Mono.class){
			Charset charset = getCharset();
			return content.map(byteBuffer -> charset.decode(byteBuffer).toString());
		} else {
			if (returnPublisherType == Mono.class) {
				return reactorObjectReader.read(content, objectReader);
			} else if(returnPublisherType == Flux.class){
				return reactorObjectReader.readElements(content, objectReader);
			} else {
				throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
			}
		}
	}

	@Override
	public Mono<Void> releaseBody() {
		return Flux.from(contentChunks).then();
	}

	private Charset getCharset() {
		return ofNullable(clientResponse.getHeaders().get(CONTENT_TYPE.asString()))
				.map(header -> {
					int pos = header.indexOf(CHARSET_DELIMITER);
					if(pos >= 0){
						return header.substring(pos + CHARSET_DELIMITER.length());
					} else {
						return null;
					}
				})
				.map(Charset::forName)
				.orElse(UTF_8);
	}

	private Flux<ByteBuffer> directContent() {
		return Flux.from(contentChunks).map(contentChunk -> contentChunk.buffer.slice());
	}

	@Override
	public Mono<byte[]> bodyData() {
		return joinChunks();
	}

	private Mono<byte[]> joinChunks() {
		return directContent().reduce(new ByteArrayOutputStream(), (baos, byteBuffer) -> {
			for(int i = byteBuffer.position(), limit = byteBuffer.limit(); i < limit; i++){
				baos.write(byteBuffer.get(i));
			}
			return baos;
		}).map(ByteArrayOutputStream::toByteArray);
	}
}
