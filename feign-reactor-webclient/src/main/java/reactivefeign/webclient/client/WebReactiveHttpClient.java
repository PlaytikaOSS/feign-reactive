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

package reactivefeign.webclient.client;

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.client.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiFunction;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static reactivefeign.utils.FeignUtils.*;

/**
 * Uses {@link WebClient} to execute http requests
 * @author Sergii Karpenko
 */
public class WebReactiveHttpClient<P extends Publisher<?>> implements ReactiveHttpClient<P> {

	private final WebClient webClient;
	private final ParameterizedTypeReference<Object> bodyActualType;
	private final BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> responseFunction;

	public static <P extends Publisher<?>> WebReactiveHttpClient<P> webClient(MethodMetadata methodMetadata, WebClient webClient) {

		Type returnPublisherType = returnPublisherType(methodMetadata);
		ParameterizedTypeReference<?> returnActualType =
				ParameterizedTypeReference.forType(returnActualType(methodMetadata));

		ParameterizedTypeReference<Object> bodyActualType = ofNullable(
				getBodyActualType(methodMetadata.bodyType()))
				.map(ParameterizedTypeReference::forType)
				.orElse(null);

		if (returnActualType.getType() instanceof ParameterizedType
				&& ((ParameterizedType) returnActualType.getType()).getRawType().equals(ResponseEntity.class)) {
			Type entityType = resolveLastTypeParameter(returnActualType.getType(), ResponseEntity.class);

			Type entityPublisherType = returnPublisherType(entityType);
			ParameterizedTypeReference<?> entityActualType =
					ParameterizedTypeReference.forType(returnActualType(entityType));

			return new WebReactiveHttpClient<>(webClient, bodyActualType,
					(request, response) -> new WebReactiveHttpEntityResponse<>(request, response, entityPublisherType, entityActualType));
		}

		return new WebReactiveHttpClient<>(webClient, bodyActualType,
				webReactiveHttpResponse(returnPublisherType, returnActualType));
	}

	public static <P extends Publisher<?>> BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> webReactiveHttpResponse(Type returnPublisherType, ParameterizedTypeReference<?> returnActualType) {
		return (request, response) -> new WebReactiveHttpResponse<>(request, response, returnPublisherType, returnActualType);
	}

	public WebReactiveHttpClient(WebClient webClient,
								 ParameterizedTypeReference<Object> bodyActualType,
								 BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> responseFunction) {
		this.webClient = webClient;
		this.bodyActualType = bodyActualType;
		this.responseFunction = responseFunction;
	}

	@Override
	public Mono<ReactiveHttpResponse<P>> executeRequest(ReactiveHttpRequest request) {
		return webClient.method(HttpMethod.valueOf(request.method()))
				.uri(request.uri())
				.headers(httpHeaders -> setUpHeaders(request, httpHeaders))
				.body(provideBody(request))
				.exchange()
				.onErrorMap(ex -> {
					if(ex instanceof io.netty.handler.timeout.ReadTimeoutException){
						return new ReadTimeoutException(ex, request);
					} else {
						return new ReactiveFeignException(ex, request);
					}
				})
				.map(response -> toReactiveHttpResponse(request, response));
	}

	protected ReactiveHttpResponse<P> toReactiveHttpResponse(ReactiveHttpRequest request, ClientResponse response) {
		return responseFunction.apply(request, response);
	}

	protected BodyInserter<?, ? super ClientHttpRequest> provideBody(ReactiveHttpRequest request) {
		return bodyActualType != null
                ? BodyInserters.fromPublisher(request.body(), bodyActualType)
                : BodyInserters.empty();
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpHeaders httpHeaders) {
		request.headers().forEach(httpHeaders::put);
	}

}
