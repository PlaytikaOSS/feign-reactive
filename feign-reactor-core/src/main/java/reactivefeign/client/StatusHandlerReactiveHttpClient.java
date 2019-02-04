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

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static reactivefeign.utils.FeignUtils.methodTag;

/**
 * Uses statusHandlers to process status of http response
 *
 * @author Sergii Karpenko
 */

public class StatusHandlerReactiveHttpClient implements ReactiveHttpClient {

  private final ReactiveHttpClient reactiveClient;
  private final String methodTag;

  private final ReactiveStatusHandler statusHandler;

  private static final ReactiveStatusHandler defaultStatusHandler = ReactiveStatusHandlers.defaultFeignErrorDecoder();

  public static ReactiveHttpClient handleStatus(
          ReactiveHttpClient reactiveClient,
          MethodMetadata methodMetadata,
          ReactiveStatusHandler statusHandler) {
    return new StatusHandlerReactiveHttpClient(reactiveClient, methodMetadata, statusHandler);
  }

  private StatusHandlerReactiveHttpClient(ReactiveHttpClient reactiveClient,
                                          MethodMetadata methodMetadata,
                                          ReactiveStatusHandler statusHandler) {
    this.reactiveClient = reactiveClient;
    this.methodTag = methodTag(methodMetadata);
    this.statusHandler = statusHandler;
  }

  @Override
  public Mono<ReactiveHttpResponse> executeRequest(ReactiveHttpRequest request) {
    return reactiveClient.executeRequest(request).map(response -> {
      if (statusHandler.shouldHandle(response.status())) {
        return new ErrorReactiveHttpResponse(response, statusHandler.decode(methodTag, response));
      } else if(defaultStatusHandler.shouldHandle(response.status())){
        return new ErrorReactiveHttpResponse(response, defaultStatusHandler.decode(methodTag, response));
      } else {
        return response;
      }
    });
  }

  private class ErrorReactiveHttpResponse extends DelegatingReactiveHttpResponse {

    private final Mono<? extends Throwable> error;

    protected ErrorReactiveHttpResponse(ReactiveHttpResponse response, Mono<? extends Throwable> error) {
      super(response);
      this.error = error;
    }

    @Override
    public Publisher<Object> body() {
      if (getResponse().body() instanceof Mono) {
        return error.flatMap(Mono::error);
      } else {
        return error.flatMapMany(Flux::error);
      }
    }
  }

}
