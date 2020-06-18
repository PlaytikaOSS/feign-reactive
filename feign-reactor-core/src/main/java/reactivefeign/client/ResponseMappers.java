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
import reactivefeign.utils.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Maps 404 error response to successful empty response
 *
 * @author Sergii Karpenko
 */
public class ResponseMappers {

  public static <P extends Publisher<?>> ReactiveHttpResponseMapper<P> ignore404() {
    return response -> {
      if (response.status() == HttpStatus.SC_NOT_FOUND) {
        return Mono.just(new DelegatingReactiveHttpResponse<P>(response) {
          @Override
          public int status() {
            return HttpStatus.SC_OK;
          }

          @Override
          public P body() {
            return (P)response.releaseBody();
          }
        });
      }
      return Mono.just(response);
    };
  }

}
