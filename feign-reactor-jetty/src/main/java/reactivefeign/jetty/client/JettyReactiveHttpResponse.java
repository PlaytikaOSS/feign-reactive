package reactivefeign.jetty.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpResponse;
import reactivejson.ReactorObjectReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;

class JettyReactiveHttpResponse implements ReactiveHttpResponse{

	public static final String CHARSET_DELIMITER = ";charset=";
	private final ReactiveResponse clientResponse;
	private final Publisher<ContentChunk> contentChunks;
	private final Class returnPublisherType;
	private Class returnActualClass;
	private final ObjectReader objectReader;
	private final JsonFactory jsonFactory;

	JettyReactiveHttpResponse(ReactiveResponse clientResponse, Publisher<ContentChunk> contentChunks,
							  Class returnPublisherType, Class returnActualClass,
							  JsonFactory jsonFactory, ObjectReader objectReader) {
		this.clientResponse = clientResponse;
		this.contentChunks = contentChunks;
		this.returnPublisherType = returnPublisherType;
		this.returnActualClass = returnActualClass;
		this.objectReader = objectReader;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public int status() {
		return clientResponse.getStatus();
	}

	@Override
	public Map<String, List<String>> headers() {
		return clientResponse.getHeaders().stream()
				.collect(Collectors.toMap(HttpField::getName, field -> asList(field.getValues())));
	}

	@Override
	public Publisher<Object> body() {
		ReactorObjectReader reactorObjectReader = new ReactorObjectReader(jsonFactory);

		Flux<ByteBuffer> content = content();

		if (returnPublisherType == Mono.class) {
			if(returnActualClass == ByteBuffer.class || returnActualClass == byte[].class){
				return joinChunks().map(ByteBuffer::wrap);
			} else if(returnActualClass.isAssignableFrom(String.class)){
				Charset charset = getCharset();
				return joinChunks().map(bytes -> charset.decode(ByteBuffer.wrap(bytes)).toString());
			} else {
				return reactorObjectReader.read(content, objectReader);
			}
		}
		else if (returnPublisherType == Flux.class) {
			if(returnActualClass == ByteBuffer.class || returnActualClass == byte[].class){
				return (Publisher)content;
			} else {
				return reactorObjectReader.readElements(content, objectReader);
			}
		}
		else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
		}
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

	private Flux<ByteBuffer> content() {
		return Flux.from(contentChunks).map(chunk -> {
			// See https://github.com/eclipse/jetty.project/issues/2429
			byte[] data = new byte[chunk.buffer.capacity()];
			chunk.buffer.get(data);
			chunk.callback.succeeded();
			return ByteBuffer.wrap(data);

//			ByteBuffer duplicate = chunk.buffer.duplicate();
//			chunk.callback.succeeded();
//			return duplicate;
		});
	}

	@Override
	public Mono<byte[]> bodyData() {
		return joinChunks();
	}

	private Mono<byte[]> joinChunks() {
		return content()
				.collect(Collectors.toList())
				.map(JettyReactiveHttpResponse::joinBuffers);
	}

	private static byte[] joinBuffers(List<ByteBuffer> buffers){
		int totalLength = buffers.stream()
				.map(ByteBuffer::limit)
				.reduce(0, (length1, length2) -> (length1 + length2));
		byte[] data = new byte[totalLength];
		int pos = 0;
		for(ByteBuffer byteBuffer : buffers){
			byteBuffer.get(data, pos, byteBuffer.capacity());
			pos += byteBuffer.capacity();
		};
		return data;
	}
}
