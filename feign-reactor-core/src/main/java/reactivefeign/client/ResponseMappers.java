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
import reactivefeign.utils.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * Maps 404 error response to successful empty response
 *
 * @author Sergii Karpenko
 */
public class ResponseMappers {

  public static BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> ignore404() {
    return (MethodMetadata methodMetadata, ReactiveHttpResponse response) -> {
      if (response.status() == HttpStatus.SC_NOT_FOUND) {
        return new DelegatingReactiveHttpResponse(response) {
          @Override
          public int status() {
            return HttpStatus.SC_OK;
          }

          @Override
          public Publisher<Object> body() {
            return Mono.empty();
          }
        };
      }
      return response;
    };
  }

  public static ReactiveHttpClient mapResponse(
          ReactiveHttpClient reactiveHttpClient,
          MethodMetadata methodMetadata,
          BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> responseMapper) {
    return request -> reactiveHttpClient.executeRequest(request)
        .map(response -> responseMapper.apply(methodMetadata, response));
  }

}
