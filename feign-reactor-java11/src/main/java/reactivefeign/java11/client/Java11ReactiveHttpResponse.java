package reactivefeign.java11.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.reactive.client.internal.QueuedSinglePublisher;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivejson.ReactorObjectReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static reactivefeign.utils.HttpUtils.CONTENT_TYPE_HEADER;

class Java11ReactiveHttpResponse implements ReactiveHttpResponse{

	public static final String CHARSET_DELIMITER = ";charset=";
	private ReactiveHttpRequest request;
	private final HttpResponse clientResponse;
	private final Publisher<List<ByteBuffer>> contentChunks;
	private final Class returnPublisherType;
	private Class<?> returnActualClass;
	private final ObjectReader objectReader;
	private final JsonFactory jsonFactory;

	Java11ReactiveHttpResponse(ReactiveHttpRequest request, HttpResponse clientResponse, Publisher<List<ByteBuffer>> contentChunks,
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
		return clientResponse.statusCode();
	}

	@Override
	public Map<String, List<String>> headers() {
		return clientResponse.headers().map();
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
		return ofNullable(headers().get(CONTENT_TYPE_HEADER))
				.map(headers -> {
					String header = headers.get(0);
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
		return Flux.from(contentChunks).concatMap(Flux::fromIterable);
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

	public static class ReactiveBodySubscriber implements Flow.Subscriber<List<ByteBuffer>> {

		private Flow.Subscription subscription;
		private QueuedSinglePublisher<List<ByteBuffer>> content = new QueuedSinglePublisher<>();

		public Flux<List<ByteBuffer>> content(){
			return Flux.from(content);
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			content.offer(item);
			subscription.request(1);
		}

		@Override
		public void onError(Throwable throwable) {
			content.fail(throwable);
		}

		@Override
		public void onComplete() {
			content.complete();

		}
	}
}
