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
package reactivefeign.client.statushandler;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.utils.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static reactivefeign.utils.FeignUtils.httpMethod;
import static reactivefeign.utils.HttpUtils.familyOf;

public class ReactiveStatusHandlers {

  public static ReactiveStatusHandler defaultFeignErrorDecoder() {
    return errorDecoder(new ErrorDecoder.Default());
  }

  public static ReactiveStatusHandler errorDecoder(ErrorDecoder errorDecoder) {
    return new ReactiveStatusHandler() {

      @Override
      public boolean shouldHandle(int status) {
        return familyOf(status).isError();
      }

      @Override
      public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse<?> response) {
        return response.bodyData()
                .defaultIfEmpty(new byte[0])
                .map(bodyData -> errorDecoder.decode(methodKey,
                        buildFeignResponseForDecoder(response, bodyData)));
      }
    };
  }

  private static Response buildFeignResponseForDecoder(ReactiveHttpResponse response, byte[] bodyData) {
    ReactiveHttpRequest request = response.request();
    Request feignRequest = Request.create(httpMethod(request.method()),
            request.uri().toString(),
            (Map)request.headers(),
            Request.Body.empty(),
            null);

    return Response.builder()
            .request(feignRequest)
            .status(response.status())
            .reason(HttpStatus.getStatusText(response.status()))
            .headers((Map)response.headers())
            .body(bodyData).build();
  }

  public static ReactiveStatusHandler throwOnStatus(
          Predicate<Integer> statusPredicate,
          BiFunction<String, ReactiveHttpResponse, Throwable> errorFunction) {
    return new ReactiveStatusHandler() {
      @Override
      public boolean shouldHandle(int status) {
        return statusPredicate.test(status);
      }

      @Override
      public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse response) {
        return Mono.just(errorFunction.apply(methodKey, response));
      }
    };
  }
}
