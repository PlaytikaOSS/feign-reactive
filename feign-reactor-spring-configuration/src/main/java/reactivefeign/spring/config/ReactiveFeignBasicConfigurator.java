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

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveErrorMapper;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpRequestInterceptors;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactivefeign.utils.Pair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class ReactiveFeignBasicConfigurator extends AbstractReactiveFeignConfigurator{


	protected ReactiveFeignBasicConfigurator() {
		super(1);
	}

	@Override
	public ReactiveFeignBuilder configure(
			ReactiveFeignBuilder builder,
			ReactiveFeignNamedContext namedContext) {

		if (isDefaultToProperties(namedContext)) {
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

	private boolean isDefaultToProperties(ReactiveFeignNamedContext namedContext){
		Map<String, ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>> config = namedContext.getProperties().getConfig();
		return Optional.ofNullable(config.get(namedContext.getClientName()))
				.map(ReactiveFeignClientsProperties.ReactiveFeignClientProperties::getDefaultToProperties)
				.orElse(namedContext.getProperties().isDefaultToProperties());
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

		ReactiveErrorMapper errorMapper = namedContext.getOptional(ReactiveErrorMapper.class);
		if(errorMapper != null){
			resultBuilder = resultBuilder.errorMapper(errorMapper);
		}

		ObjectMapper objectMapper = namedContext.getOptional(ObjectMapper.class);
		if(objectMapper != null){
			resultBuilder = resultBuilder.objectMapper(objectMapper);
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
			for (Map.Entry<String, List<String>> headerPair : config.getDefaultRequestHeaders().entrySet()) {
				// Every Map headerPair is gonna belong to it's own interceptor
				List<Pair<String, String>> headerSubPairs = headerPair.getValue().stream()
								.map(value -> new Pair<>(headerPair.getKey(), value))
								.collect(Collectors.toList());
				resultBuilder.addRequestInterceptor(ReactiveHttpRequestInterceptors.addHeaders(headerSubPairs));
			}
		}

		if (config.getDefaultQueryParameters() != null) {
			for (Map.Entry<String, List<String>> queryPair : config.getDefaultQueryParameters().entrySet()) {
				// Every Map queryPair is gonna belong to it's own interceptor
                List<Pair<String, String>> querySubPairs = queryPair.getValue().stream()
                        .map(value -> new Pair<>(queryPair.getKey(), value))
                        .collect(Collectors.toList());
                resultBuilder.addRequestInterceptor(ReactiveHttpRequestInterceptors.addQueries(querySubPairs));
			}
		}

		if (config.getStatusHandler() != null) {
			ReactiveStatusHandler statusHandler = namedContext.getOrInstantiate(config.getStatusHandler());
			resultBuilder = resultBuilder.statusHandler(statusHandler);
		} else if(config.getErrorDecoder() != null){
			ErrorDecoder errorDecoder = namedContext.getOrInstantiate(config.getErrorDecoder());
			resultBuilder = resultBuilder.statusHandler(ReactiveStatusHandlers.errorDecoder(errorDecoder));
		}

		if (config.getErrorMapper() != null) {
			ReactiveErrorMapper errorMapper = namedContext.getOrInstantiate(config.getErrorMapper());
			resultBuilder = resultBuilder.errorMapper(errorMapper);
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
}
