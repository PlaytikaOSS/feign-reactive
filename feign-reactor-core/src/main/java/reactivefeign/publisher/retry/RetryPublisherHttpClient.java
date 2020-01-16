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

import feign.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.publisher.PublisherHttpClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static reactivefeign.utils.FeignUtils.methodTag;

/**
 * Wraps {@link PublisherHttpClient} with retry logic provided by retryFunction
 *
 * @author Sergii Karpenko
 */
abstract public class RetryPublisherHttpClient implements PublisherHttpClient {

  private static final Logger logger = LoggerFactory.getLogger(RetryPublisherHttpClient.class);

  private final String feignMethodTag;
  protected final PublisherHttpClient publisherClient;
  protected final Function<Flux<Throwable>, Flux<Throwable>> retryFunction;

  protected RetryPublisherHttpClient(PublisherHttpClient publisherClient,
                                   MethodMetadata methodMetadata,
                                   Function<Flux<Throwable>, Flux<Throwable>> retryFunction) {
    this.publisherClient = publisherClient;
    this.feignMethodTag = methodTag(methodMetadata);
    this.retryFunction = wrapWithLog(retryFunction, feignMethodTag);
  }

  protected Function<Flux<Throwable>, Flux<Throwable>> wrapWithOutOfRetries(
          Function<Flux<Throwable>, Flux<Throwable>> retryFunction,
          ReactiveHttpRequest request){
     return throwableFlux -> retryFunction.apply(throwableFlux)
             .onErrorResume(throwable -> Mono.just(new OutOfRetriesWrapper(throwable, request)))
             .zipWith(Flux.range(1, Integer.MAX_VALUE), (throwable, index) -> {
               if(throwable instanceof OutOfRetriesWrapper){
                 if(index == 1){
                   throw Exceptions.propagate(throwable.getCause());
                 } else {
                   logger.error("[{}]---> USED ALL RETRIES", feignMethodTag, throwable);
                   throw Exceptions.propagate(new OutOfRetriesException(throwable.getCause(), request));
                 }
               } else {
                 return throwable;
               }
             });
  }

  protected static Function<Flux<Throwable>, Flux<Throwable>> wrapWithLog(
          Function<Flux<Throwable>, Flux<Throwable>> retryFunction,
          String feignMethodTag) {
    return throwableFlux -> retryFunction.apply(throwableFlux)
            .doOnNext(throwable -> {
              if (logger.isDebugEnabled()) {
                logger.debug("[{}]---> RETRYING on error", feignMethodTag, throwable);
              }
            });
  }

  private static class OutOfRetriesWrapper extends ReactiveFeignException {

      public OutOfRetriesWrapper(Throwable cause, ReactiveHttpRequest request) {
          super(cause, request);
      }
  }
}
