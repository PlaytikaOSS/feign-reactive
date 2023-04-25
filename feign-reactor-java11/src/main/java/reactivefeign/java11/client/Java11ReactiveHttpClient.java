/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.java11.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.utils.SerializedFormData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static java.net.http.HttpResponse.BodyHandlers.fromSubscriber;
import static java.nio.charset.StandardCharsets.UTF_8;
import static reactivefeign.utils.CollectionUtils.isEmpty;
import static reactivefeign.utils.FeignUtils.getBodyActualType;
import static reactivefeign.utils.FeignUtils.returnActualType;
import static reactivefeign.utils.FeignUtils.returnPublisherType;
import static reactivefeign.utils.HttpUtils.ACCEPT_ENCODING_HEADER;
import static reactivefeign.utils.HttpUtils.ACCEPT_HEADER;
import static reactivefeign.utils.HttpUtils.APPLICATION_JSON;
import static reactivefeign.utils.HttpUtils.APPLICATION_JSON_UTF_8;
import static reactivefeign.utils.HttpUtils.APPLICATION_OCTET_STREAM;
import static reactivefeign.utils.HttpUtils.APPLICATION_STREAM_JSON;
import static reactivefeign.utils.HttpUtils.APPLICATION_STREAM_JSON_UTF_8;
import static reactivefeign.utils.HttpUtils.CONTENT_TYPE_HEADER;
import static reactivefeign.utils.HttpUtils.GZIP;
import static reactivefeign.utils.HttpUtils.NEWLINE_SEPARATOR;
import static reactivefeign.utils.HttpUtils.TEXT;
import static reactivefeign.utils.HttpUtils.TEXT_UTF_8;
import static reactor.adapter.JdkFlowAdapter.publisherToFlowPublisher;

/**
 * Uses reactive Java 11 client to execute http requests
 * @author Sergii Karpenko
 */
public class Java11ReactiveHttpClient implements ReactiveHttpClient {

	private final HttpClient httpClient;
	private final Class bodyActualClass;
	private final Class returnPublisherClass;
	private final Class returnActualClass;
	private final JsonFactory jsonFactory;
	private final ObjectWriter bodyWriter;
	private final ObjectReader responseReader;
	private long requestTimeout = -1;
	private boolean tryUseCompression = false;

	public static Java11ReactiveHttpClient java11Client(
			MethodMetadata methodMetadata,
			HttpClient httpClient,
			JsonFactory jsonFactory, ObjectMapper objectMapper) {

		Class returnPublisherType = returnPublisherType(methodMetadata);
		Type returnActualType = returnActualType(methodMetadata);
		Type bodyActualType = getBodyActualType(methodMetadata.bodyType());
		ObjectWriter bodyWriter = bodyActualType != null
				? objectMapper.writerFor(objectMapper.constructType(bodyActualType)) : null;
		ObjectReader responseReader = objectMapper.readerFor(objectMapper.constructType(returnActualType));

		return new Java11ReactiveHttpClient(httpClient,
				getClass(bodyActualType), returnPublisherType, getClass(returnActualType),
				jsonFactory, bodyWriter, responseReader);
	}

	public Java11ReactiveHttpClient(HttpClient httpClient,
									Class bodyActualClass, Class returnPublisherClass, Class returnActualClass,
									JsonFactory jsonFactory, ObjectWriter bodyWriter, ObjectReader responseReader) {
		this.httpClient = httpClient;
		this.bodyActualClass = bodyActualClass;
		this.returnPublisherClass = returnPublisherClass;
		this.returnActualClass = returnActualClass;
		this.jsonFactory = jsonFactory;
		this.bodyWriter = bodyWriter;
		this.responseReader = responseReader;
	}

	public Java11ReactiveHttpClient setRequestTimeout(long timeoutInMillis){
		this.requestTimeout = timeoutInMillis;
		return this;
	}

	public Java11ReactiveHttpClient setTryUseCompression(boolean tryUseCompression){
		this.tryUseCompression = tryUseCompression;
		return this;
	}

	@Override
	public Mono<ReactiveHttpResponse> executeRequest(ReactiveHttpRequest request) {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(request.uri())
				.method(request.method().toUpperCase(), provideBody(request));
		setUpHeaders(request, requestBuilder);

		if(requestTimeout > 0){
			requestBuilder = requestBuilder.timeout(Duration.ofMillis(requestTimeout));
		}
		if(tryUseCompression){
			requestBuilder = requestBuilder.setHeader(ACCEPT_ENCODING_HEADER, GZIP);
		}

		Java11ReactiveHttpResponse.ReactiveBodySubscriber bodySubscriber = new Java11ReactiveHttpResponse.ReactiveBodySubscriber();

		CompletableFuture<HttpResponse<Void>> response = httpClient.sendAsync(
				requestBuilder.build(), fromSubscriber(bodySubscriber));

		return Mono.fromFuture(response)
				.<ReactiveHttpResponse>map(resp -> {
					if(!resp.version().equals(httpClient.version())){
						throw new IllegalArgumentException("Incorrect response version:"+resp.version());
					}
					return new Java11ReactiveHttpResponse(request, resp, bodySubscriber.content(),
							returnPublisherClass, returnActualClass,
							jsonFactory, responseReader);
				})
				.onErrorMap(ex -> {
					if(ex instanceof java.net.http.HttpTimeoutException){
						return new ReadTimeoutException(ex, request);
					} else {
						return new ReactiveFeignException(ex, request);
					}
				});
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpRequest.Builder requestBuilder) {
		request.headers().forEach((key, values) -> values.forEach(value -> requestBuilder.header(key, value)));

		if(isEmpty(request.headers().get(CONTENT_TYPE_HEADER))){
			String contentTypeHeader = getContentTypeHeader(request);
			if(contentTypeHeader != null) {
				requestBuilder.header(CONTENT_TYPE_HEADER, contentTypeHeader);
			}
		}

		if(isEmpty(request.headers().get(ACCEPT_HEADER))){
			requestBuilder.header(ACCEPT_HEADER, getAcceptHeader());
		}

	}

	private String getAcceptHeader() {
		String acceptHeader;
		if(CharSequence.class.isAssignableFrom(returnActualClass) && returnPublisherClass == Mono.class){
			acceptHeader = TEXT;
		}
		else if(returnActualClass == ByteBuffer.class || returnActualClass == byte[].class){
			acceptHeader = APPLICATION_OCTET_STREAM;
		}
		else if(returnPublisherClass == Mono.class){
			acceptHeader = APPLICATION_JSON;
		}
		else {
			acceptHeader = APPLICATION_STREAM_JSON;
		}
		return acceptHeader;
	}

	private String getContentTypeHeader(ReactiveHttpRequest request) {
		String contentType;
		if(bodyActualClass == null){
			return null;
		}

		if(request.body() instanceof Mono){
			if(bodyActualClass == ByteBuffer.class){
				contentType = APPLICATION_OCTET_STREAM;
			}
			else if (CharSequence.class.isAssignableFrom(bodyActualClass)){
				contentType = TEXT_UTF_8;
			}
			else {
				contentType = APPLICATION_JSON_UTF_8;
			}
		} else {
			if(bodyActualClass == ByteBuffer.class){
				contentType = APPLICATION_OCTET_STREAM;
			}
			else {
				contentType = APPLICATION_STREAM_JSON_UTF_8;
			}
		}
		return contentType;
	}

	protected HttpRequest.BodyPublisher provideBody(ReactiveHttpRequest request) {
		if(bodyActualClass != null || request.body() instanceof SerializedFormData) {
			Publisher<ByteBuffer> bodyPublisher;
			if (request.body() instanceof SerializedFormData) {
				bodyPublisher = Mono.just(((SerializedFormData) request.body()).getFormData());
			} else if (request.body() instanceof Mono) {
				if (bodyActualClass == ByteBuffer.class) {
					bodyPublisher = (Mono) request.body();
				} else if (CharSequence.class.isAssignableFrom(bodyActualClass)) {
					bodyPublisher = Flux.from(request.body()).map(this::toCharSequenceChunk);
				} else {
					bodyPublisher = Flux.from(request.body()).map(data -> toJsonChunk(data, false));
				}

			} else {
				if (bodyActualClass == ByteBuffer.class) {
					bodyPublisher = (Publisher) request.body();
				} else {
					bodyPublisher = Flux.from(request.body()).map(data -> toJsonChunk(data, true));
				}
			}

			return HttpRequest.BodyPublishers.fromPublisher(publisherToFlowPublisher(bodyPublisher));
		} else {
			return HttpRequest.BodyPublishers.noBody();
		}
	}

	protected ByteBuffer toCharSequenceChunk(Object data){
		CharBuffer charBuffer = CharBuffer.wrap((CharSequence) data);
		return UTF_8.encode(charBuffer);
	}

	protected ByteBuffer toJsonChunk(Object data, boolean stream){
		try {
			ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();
			bodyWriter.writeValue(byteArrayBuilder, data);
			if(stream) {
				byteArrayBuilder.write(NEWLINE_SEPARATOR);
			}
			return ByteBuffer.wrap(byteArrayBuilder.toByteArray());
		} catch (java.io.IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Class getClass(Type type){
		return (Class)(type instanceof ParameterizedType
				? ((ParameterizedType) type).getRawType() : type);
	}
}
