/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.publisher.retry;

import feign.ExceptionPropagationPolicy;
import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


/**
 * Wraps {@link PublisherHttpClient} with retry logic provided by retryFunction
 *
 * @author Sergii Karpenko
 */
abstract public class RetryPublisherHttpClient implements PublisherHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(RetryPublisherHttpClient.class);

    protected final String feignMethodKey;
    protected final PublisherHttpClient publisherClient;
    private final Retry retry;
    private final ReactiveRetryPolicy retryPolicy;

    private final ExceptionPropagationPolicy exceptionPropagationPolicy;

    protected RetryPublisherHttpClient(
            PublisherHttpClient publisherClient,
            MethodMetadata methodMetadata,
            ReactiveRetryPolicy retryPolicy) {

        this.publisherClient = publisherClient;
        this.feignMethodKey = methodMetadata.configKey();
        this.retry = wrapWithRetryLog(retryPolicy.retry(), feignMethodKey);
        this.exceptionPropagationPolicy = retryPolicy.exceptionPropagationPolicy();
        this.retryPolicy = retryPolicy;
    }

    protected Retry getRetry(ReactiveHttpRequest request){
        return wrapWithOutOfRetriesLog(request);
    }

    private Retry wrapWithOutOfRetriesLog(ReactiveHttpRequest request) {
        return new Retry() {
            @Override
            public Publisher<?> generateCompanion(Flux<RetrySignal> retrySignals) {
                return Flux.<Object>from(retry.generateCompanion(retrySignals))
                        .onErrorResume(throwable -> Mono.just(new OutOfRetriesWrapper(throwable, request)))
                        .zipWith(Flux.range(1, retryPolicy.maxAllowedRetries() + 1), (object, index) -> {
                            if(object instanceof OutOfRetriesWrapper){
                                OutOfRetriesWrapper wrapper = (OutOfRetriesWrapper) object;
                                if(index == 1){
                                    throw Exceptions.propagate(wrapper.getCause());
                                } else {
                                    logger.debug("[{}]---> USED ALL RETRIES", feignMethodKey, wrapper.getCause());
                                    throw Exceptions.propagate(
                                            exceptionPropagationPolicy == ExceptionPropagationPolicy.UNWRAP
                                                    ? wrapper.getCause()
                                                    : new OutOfRetriesException(wrapper.getCause(), request));
                                }
                            } else {
                                return object;
                            }
                        });
            }
        };
    }

    private static Retry wrapWithRetryLog(Retry retry, String feignMethodTag) {
        if(logger.isDebugEnabled()){
            return new Retry() {
                @Override
                public Publisher<?> generateCompanion(Flux<RetrySignal> retrySignals) {
                    Flux<RetrySignal> cache = retrySignals.cache();
                    return cache.zipWith(retry.generateCompanion(cache))
                            .map(tuple -> {
                                Throwable failure = tuple.getT1().failure();
                                logger.debug("[{}]---> RETRYING on error", feignMethodTag, failure);
                                return tuple.getT2();
                            });
                }
            };
        } else {
            return retry;
        }

    }

    private static class OutOfRetriesWrapper extends ReactiveFeignException {

        public OutOfRetriesWrapper(Throwable cause, ReactiveHttpRequest request) {
            super(cause, request);
        }
    }
}
