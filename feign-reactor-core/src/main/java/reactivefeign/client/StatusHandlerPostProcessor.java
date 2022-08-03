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
package reactivefeign.client;

import org.reactivestreams.Publisher;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

/**
 * Uses statusHandlers to process status of http response
 *
 * @author Sergii Karpenko
 */

public class StatusHandlerPostProcessor<P extends Publisher<?>> implements ReactiveHttpResponseMapper<P> {

  private final ReactiveStatusHandler statusHandler;

  private static final ReactiveStatusHandler defaultStatusHandler = ReactiveStatusHandlers.defaultFeignErrorDecoder();

  public static StatusHandlerPostProcessor handleStatus(ReactiveStatusHandler statusHandler) {
    return new StatusHandlerPostProcessor(statusHandler);
  }

  private StatusHandlerPostProcessor(ReactiveStatusHandler statusHandler) {
    this.statusHandler = statusHandler;
  }

  @Override
  public Mono<ReactiveHttpResponse<P>> apply(ReactiveHttpResponse<P> response) {
    String methodKey = response.request().methodKey();
    if (statusHandler.shouldHandle(response.status())) {
      return statusHandler.decode(methodKey, response)
              .map(throwable -> {throw Exceptions.propagate(throwable);});
    } else if(defaultStatusHandler.shouldHandle(response.status())){
      return defaultStatusHandler.decode(methodKey, response)
              .map(throwable -> {throw Exceptions.propagate(throwable);});
    }
    return Mono.just(response);
  }
}
