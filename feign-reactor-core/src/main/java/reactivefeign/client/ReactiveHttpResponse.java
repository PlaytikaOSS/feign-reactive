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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive response from an http server.
 * 
 * @author Sergii Karpenko
 */
public interface ReactiveHttpResponse<P extends Publisher<?>> {

  ReactiveHttpRequest request();

  int status();

  Map<String, List<String>> headers();

  P body();

  Mono<Void> releaseBody();

  /**
   * used by error decoders
   * 
   * @return error message data
   */
  Mono<byte[]> bodyData();
}
