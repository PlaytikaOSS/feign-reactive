/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package reactivefeign.spring.config;

import feign.codec.ErrorDecoder;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ReactiveFeignBasicConfigurator extends AbstractReactiveFeignConfigurator{
	private static final String QUERY_PAIRS_SEPARATOR = "&";
	public static final String QUERY_KEY_VALUE_SEPARATOR = "=";

	protected ReactiveFeignBasicConfigurator() {
		super(1);
	}

	@Override
	public ReactiveFeignBuilder configure(
			ReactiveFeignBuilder builder,
			ReactiveFeignNamedContext namedContext) {

		if (namedContext.getProperties().isDefaultToProperties()) {
			builder = configureUsingConfiguration(builder, namedContext);
			for(ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?> config : namedContext.getConfigsReverted()){
				builder = configureUsingProperties(builder, namedContext, config);
			}
		} else {
			for(ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?> config : namedContext.getConfigsReverted()){
				builder = configureUsingProperties(builder, namedContext, config);
			}
			builder = configureUsingConfiguration(builder, namedContext);
		}
		return builder;
	}

	private ReactiveFeignBuilder configureUsingConfiguration(ReactiveFeignBuilder builder, ReactiveFeignNamedContext namedContext) {
		ReactiveFeignBuilder resultBuilder = builder;

		ReactiveOptions options = namedContext.getOptional(ReactiveOptions.class);
		if (options != null) {
			resultBuilder = resultBuilder.options(options);
		}

		ReactiveRetryPolicy retryPolicy = namedContext.getOptional(ReactiveRetryPolicy.class);
		if (retryPolicy != null) {
			resultBuilder = resultBuilder.retryWhen(retryPolicy);
		}

		Map<String, ReactiveHttpRequestInterceptor> requestInterceptors = namedContext.getAll(ReactiveHttpRequestInterceptor.class);
		if (requestInterceptors != null) {
			for(ReactiveHttpRequestInterceptor interceptor : requestInterceptors.values()){
				resultBuilder = resultBuilder.addRequestInterceptor(interceptor);
			}
		}

		ReactiveStatusHandler statusHandler = namedContext.getOptional(ReactiveStatusHandler.class);
		if(statusHandler == null){
			ErrorDecoder errorDecoder = namedContext.getOptional(ErrorDecoder.class);
			if(errorDecoder != null) {
				statusHandler = ReactiveStatusHandlers.errorDecoder(errorDecoder);
			}
		}
		if (statusHandler != null) {
			resultBuilder = resultBuilder.statusHandler(statusHandler);
		}

		namedContext.getAll(ReactiveLoggerListener.class).values()
				.forEach(resultBuilder::addLoggerListener);

		return resultBuilder;
	}

	private ReactiveFeignBuilder configureUsingProperties(
			ReactiveFeignBuilder builder,
			ReactiveFeignNamedContext namedContext,
			ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?> config){

		ReactiveFeignBuilder resultBuilder = builder;

		if (config == null) {
			return resultBuilder;
		}

		ReactiveOptions.Builder optionsBuilder = config.getOptions();
		if(optionsBuilder != null){
			resultBuilder = resultBuilder.options(optionsBuilder.build());
		}

		if (config.getRetry() != null) {
			ReactiveRetryPolicy retryPolicy = configureRetryPolicyFromProperties(namedContext, config.getRetry());
			resultBuilder = resultBuilder.retryWhen(retryPolicy);
		}

		if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
			// this will add request interceptor to builder, not replace existing
			for (Class<ReactiveHttpRequestInterceptor> interceptorClass : config.getRequestInterceptors()) {
				ReactiveHttpRequestInterceptor interceptor = namedContext.getOrInstantiate(interceptorClass);
				resultBuilder = resultBuilder.addRequestInterceptor(interceptor);
			}
		}

		if (config.getDefaultRequestHeaders() != null) {
			for (Map.Entry<String, List<String>> entry : config.getDefaultRequestHeaders().entrySet()) {
				// Every Map entry is gonna belong to it's own interceptor
				resultBuilder = resultBuilder.addRequestInterceptor(reactiveHttpRequest -> {
					reactiveHttpRequest.headers().put(entry.getKey(), entry.getValue());
					return Mono.just(reactiveHttpRequest);
				});
			}
		}

		if (config.getDefaultQueryParameters() != null) {
			for (Map.Entry<String, List<String>> entry : config.getDefaultQueryParameters().entrySet()) {
				// Every Map entry is gonna belong to it's own interceptor
				resultBuilder = resultBuilder.addRequestInterceptor(reactiveHttpRequest -> {
					for (String value : entry.getValue()) {
						reactiveHttpRequest = reactiveHttpRequestWithQuery(reactiveHttpRequest, entry.getKey(), value);
					}
					return Mono.just(reactiveHttpRequest);
				});
			}
		}

		if (config.getStatusHandler() != null) {
			ReactiveStatusHandler statusHandler = namedContext.getOrInstantiate(config.getStatusHandler());
			resultBuilder = resultBuilder.statusHandler(statusHandler);
		} else if(config.getErrorDecoder() != null){
			ErrorDecoder errorDecoder = namedContext.getOrInstantiate(config.getErrorDecoder());
			resultBuilder = resultBuilder.statusHandler(ReactiveStatusHandlers.errorDecoder(errorDecoder));
		}

		if(config.getLogger() != null){
			resultBuilder = resultBuilder.addLoggerListener(namedContext.getOrInstantiate(config.getLogger()));
		}

		if(config.getMetricsLogger() != null){
			resultBuilder = resultBuilder.addLoggerListener(namedContext.getOrInstantiate(config.getMetricsLogger()));
		}

		if (config.getDecode404() != null && config.getDecode404()) {
			resultBuilder = resultBuilder.decode404();
		}

		if (Objects.nonNull(config.getContract())) {
			resultBuilder = resultBuilder.contract(namedContext.getOrInstantiate(config.getContract()));
		}
		return resultBuilder;
	}

	static ReactiveRetryPolicy configureRetryPolicyFromProperties(
	        ReactiveFeignNamedContext namedContext, ReactiveFeignClientsProperties.RetryProperties retryProperties) {
		ReactiveRetryPolicy retryPolicy = null;
		if(retryProperties.getPolicy() != null){
			retryPolicy = namedContext.getOrInstantiate(retryProperties.getPolicy());
		}
		if(retryPolicy == null){
			ReactiveRetryPolicy.Builder retryPolicyBuilder = namedContext.getOrInstantiate(
					retryProperties.getBuilder(), retryProperties.getArgs());
			retryPolicy = retryPolicyBuilder.build();
		}
		return retryPolicy;
	}

	private static ReactiveHttpRequest reactiveHttpRequestWithQuery(ReactiveHttpRequest reactiveHttpRequest, String key, String value) {
		URI uri = reactiveHttpRequest.uri();
		String query = uri.getQuery();
		String keyValuePair = key + QUERY_KEY_VALUE_SEPARATOR + value;

		if (query == null) {
			query = keyValuePair;
		} else {
			query += QUERY_PAIRS_SEPARATOR + keyValuePair;
		}

		try {
			return new ReactiveHttpRequest(reactiveHttpRequest, new URI(uri.getScheme(), uri.getAuthority(),
					uri.getPath(), query, uri.getFragment()));
		} catch (URISyntaxException e) {
			// Ignore error with malformed URL, cannot be sent here
			return reactiveHttpRequest;
		}
	}
}
