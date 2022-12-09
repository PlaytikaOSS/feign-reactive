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
import feign.Target;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * An immutable reactive request to an http server.
 * 
 * @author Sergii Karpenko
 */
public final class ReactiveHttpRequest {

  private final MethodMetadata methodMetadata;
  private final Target<?> target;
  private final URI uri;
  private final Map<String, List<String>> headers;
  private final Publisher<Object> body;

  /**
   * No parameters can be null except {@code body}. All parameters must be effectively immutable,
   * via safe copies, not mutating or otherwise.
   */
  public ReactiveHttpRequest(MethodMetadata methodMetadata, Target<?> target,
                             URI uri, Map<String, List<String>> headers, Publisher<Object> body) {
    this.methodMetadata = checkNotNull(methodMetadata, "method of %s", uri);
    this.target = checkNotNull(target, "target of %s", uri);
    this.uri = checkNotNull(uri, "url");
    this.headers = checkNotNull(headers, "headers of %s %s", methodMetadata, uri);
    this.body = body; // nullable
  }

  public ReactiveHttpRequest(ReactiveHttpRequest request, URI uri) {
    this(request.methodMetadata, request.target, uri, request.headers, request.body);
  }

  public ReactiveHttpRequest(ReactiveHttpRequest request, Publisher<Object> body){
     this(request.methodMetadata, request.target, request.uri, request.headers, body);
  }

  /* Method to invoke on the server. */
  public String method() {
    return methodMetadata.template().method();
  }



  /* Fully resolved URL including query. */
  public URI uri() {
    return uri;
  }

  /* Ordered list of headers that will be sent to the server. */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * If present, this is the replayable body to send to the server.
   */
  public Publisher<Object> body() {
    return body;
  }

  public String methodKey(){
    return methodMetadata.configKey();
  }

  public Target<?> target() {
    return target;
  }

  @Override
  public String toString() {
    return "ReactiveHttpRequest{" +
            ", uri=" + uri +
            ", target=" + target +
            "}";
  }
}
