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

package reactivefeign.jetty.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import feign.MethodMetadata;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.ReadTimeoutException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;

import static feign.Util.resolveLastTypeParameter;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.eclipse.jetty.http.HttpHeader.ACCEPT;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static reactivefeign.jetty.utils.ProxyPostProcessor.postProcess;
import static reactivefeign.utils.FeignUtils.getBodyActualType;

/**
 * Uses reactive Jetty client to execute http requests
 * @author Sergii Karpenko
 */
public class JettyReactiveHttpClient implements ReactiveHttpClient {

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	public static final String TEXT = "text/plain";
	public static final String TEXT_UTF_8 = TEXT+";charset=utf-8";

	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_JSON_UTF_8 = APPLICATION_JSON+";charset=utf-8";
	public static final String APPLICATION_STREAM_JSON = "application/stream+json";
	public static final String APPLICATION_STREAM_JSON_UTF_8 = APPLICATION_STREAM_JSON+";charset=utf-8";


	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	private final HttpClient httpClient;
	private final Class bodyActualClass;
	private final Class returnPublisherClass;
	private final Class returnActualClass;
	private final JsonFactory jsonFactory;
	private final ObjectWriter bodyWriter;
	private final ObjectReader responseReader;
	private long requestTimeout = -1;

	public static JettyReactiveHttpClient jettyClient(
			MethodMetadata methodMetadata,
			HttpClient httpClient,
			JsonFactory jsonFactory, ObjectMapper objectMapper) {

		final Type returnType = methodMetadata.returnType();
		Class returnPublisherType = (Class)((ParameterizedType) returnType).getRawType();
		Class returnActualType = getClass(resolveLastTypeParameter(returnType, returnPublisherType));
		Class bodyActualType = getClass(getBodyActualType(methodMetadata.bodyType()));

		return new JettyReactiveHttpClient(httpClient,
				bodyActualType, returnPublisherType, returnActualType,
				jsonFactory,
				objectMapper.writerFor(bodyActualType),
				objectMapper.readerFor(returnActualType));
	}

	public JettyReactiveHttpClient(HttpClient httpClient,
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

	public JettyReactiveHttpClient setRequestTimeout(long timeoutInMillis){
		this.requestTimeout = timeoutInMillis;
		return this;
	}

	@Override
	public Mono<ReactiveHttpResponse> executeRequest(ReactiveHttpRequest request) {
		Request jettyRequest = httpClient.newRequest(request.uri()).method(request.method());
		setUpHeaders(request, jettyRequest.getHeaders());
		if(requestTimeout > 0){
			jettyRequest.timeout(requestTimeout, TimeUnit.MILLISECONDS);
		}

		ReactiveRequest.Builder requestBuilder = ReactiveRequest.newBuilder(jettyRequest);
		if(bodyActualClass != null){
			ReactiveRequest.Content content = provideBody(request);
			requestBuilder.content(content);
			jettyRequest.getHeaders().put(CONTENT_TYPE.asString(), singletonList(content.getContentType()));
		}

		return Mono.<ReactiveHttpResponse>from(requestBuilder.build().response((response, content) -> Mono.just(
				new JettyReactiveHttpResponse(response.getResponse(),
						postProcess(content,
								(contentChunk, throwable) -> {
									if(throwable != null){
										contentChunk.callback.failed(throwable);
									} else {
										contentChunk.callback.succeeded();
									}
								}),
						returnPublisherClass, returnActualClass,
						jsonFactory, responseReader))


		)).onErrorMap(ex -> ex instanceof java.util.concurrent.TimeoutException,
				ReadTimeoutException::new);
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpFields httpHeaders) {
		request.headers().forEach(httpHeaders::put);

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
		httpHeaders.put(ACCEPT.asString(), singletonList(acceptHeader));
	}

	protected ReactiveRequest.Content provideBody(ReactiveHttpRequest request) {
		Publisher<ContentChunk> bodyPublisher;
		String contentType;
		if(request.body() instanceof Mono){
			if(bodyActualClass == ByteBuffer.class){
				bodyPublisher = ((Mono)request.body()).map(this::toByteBufferChunk);
				contentType = APPLICATION_OCTET_STREAM;
			}
			else if(bodyActualClass == byte[].class){
				bodyPublisher = Flux.from(request.body()).map(this::toByteArrayChunk);
				contentType = APPLICATION_OCTET_STREAM;
			}
			else if (CharSequence.class.isAssignableFrom(bodyActualClass)){
				bodyPublisher = Flux.from(request.body()).map(this::toCharSequenceChunk);
				contentType = TEXT_UTF_8;
			}
			else {
				bodyPublisher = Flux.from(request.body()).map(data -> toJsonChunk(data, false));
				contentType = APPLICATION_JSON_UTF_8;
			}

		} else {
			if(bodyActualClass == ByteBuffer.class){
				bodyPublisher = Flux.from(request.body()).map(this::toByteBufferChunk);
				contentType = APPLICATION_OCTET_STREAM;
			}
			else if(bodyActualClass == byte[].class){
				bodyPublisher = Flux.from(request.body()).map(this::toByteArrayChunk);
				contentType = APPLICATION_OCTET_STREAM;
			}
			else {
				bodyPublisher = Flux.from(request.body()).map(data -> toJsonChunk(data, true));
				contentType = APPLICATION_STREAM_JSON_UTF_8;
			}
		}

		return ReactiveRequest.Content.fromPublisher(bodyPublisher, contentType);
	}

	protected ContentChunk toByteBufferChunk(Object data){
		return new ContentChunk((ByteBuffer)data);
	}

	protected ContentChunk toByteArrayChunk(Object data){
		return new ContentChunk(ByteBuffer.wrap((byte[])data));
	}

	protected ContentChunk toCharSequenceChunk(Object data){
		CharBuffer charBuffer = CharBuffer.wrap((CharSequence) data);
		ByteBuffer byteBuffer = UTF_8.encode(charBuffer);
		return new ContentChunk(byteBuffer);
	}

	protected ContentChunk toJsonChunk(Object data, boolean stream){
		try {
			ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();
			bodyWriter.writeValue(byteArrayBuilder, data);
			if(stream) {
				byteArrayBuilder.write(NEWLINE_SEPARATOR);
			}
			ByteBuffer buffer = ByteBuffer.wrap(byteArrayBuilder.toByteArray());
			return new ContentChunk(buffer);
		} catch (java.io.IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Class getClass(Type type){
		return (Class)(type instanceof ParameterizedType
				? ((ParameterizedType) type).getRawType() : type);
	}
}
