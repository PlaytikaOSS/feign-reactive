package reactivefeign.jetty.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpResponse;
import reactivejson.ReactorObjectReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
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
	private final Response clientResponse;
	private final Publisher<ContentChunk> contentChunks;
	private final Class returnPublisherType;
	private Class<?> returnActualClass;
	private final ObjectReader objectReader;
	private final JsonFactory jsonFactory;

	JettyReactiveHttpResponse(Response clientResponse, Publisher<ContentChunk> contentChunks,
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
