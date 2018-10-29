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
package reactivefeign.resttemplate.client;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;

import static java.util.Optional.ofNullable;

/**
 * {@link RestTemplate} based implementation
 *
 * @author Sergii Karpenko
 */
public class RestTemplateFakeReactiveFeign {

  public static <T> ReactiveFeign.Builder<T> builder() {
    return new ReactiveFeign.Builder<T>(){

      {
        clientFactory(methodMetadata -> new RestTemplateFakeReactiveHttpClient(
                methodMetadata, new RestTemplate(), false));
      }

      @Override
      public ReactiveFeign.Builder<T> options(ReactiveOptions options) {
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(
                        HttpClientBuilder.create().build());
        if (options.getConnectTimeoutMillis() != null) {
          requestFactory.setConnectTimeout(options.getConnectTimeoutMillis().intValue());
        }
        if (options.getReadTimeoutMillis() != null) {
          requestFactory.setReadTimeout(options.getReadTimeoutMillis().intValue());
        }

        this.clientFactory(methodMetadata -> {
          boolean acceptGzip = ofNullable(options.isTryUseCompression()).orElse(false);
          return new RestTemplateFakeReactiveHttpClient(
                  methodMetadata, new RestTemplate(requestFactory), acceptGzip);
        });

        return this;
      }
    };
  }
}


