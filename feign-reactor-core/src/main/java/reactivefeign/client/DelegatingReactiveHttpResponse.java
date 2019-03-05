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

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Sergii Karpenko
 */
abstract public class DelegatingReactiveHttpResponse implements ReactiveHttpResponse {

  private final ReactiveHttpResponse response;

  protected DelegatingReactiveHttpResponse(ReactiveHttpResponse response) {
    this.response = response;
  }

  protected ReactiveHttpResponse getResponse() {
    return response;
  }

  @Override
  public ReactiveHttpRequest request() {
    return response.request();
  }

  @Override
  public int status() {
    return response.status();
  }

  @Override
  public Map<String, List<String>> headers() {
    return response.headers();
  }

  @Override
  public Mono<byte[]> bodyData() {
    throw new UnsupportedOperationException();
  }
}
