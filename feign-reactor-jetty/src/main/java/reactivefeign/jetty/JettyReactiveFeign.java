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
package reactivefeign.jetty;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.client.HttpClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;
import reactivefeign.jetty.client.JettyReactiveHttpClient;

/**
 * Reactive Jetty client based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public class JettyReactiveFeign {

  public static <T> Builder<T> builder() {
      try {
          HttpClient httpClient = new HttpClient();
          httpClient.start();
          ObjectMapper objectMapper = new ObjectMapper();
          objectMapper.registerModule(new JavaTimeModule());
          return new Builder<>(httpClient, new JsonFactory(), objectMapper);
      } catch (Exception e) {
          throw new RuntimeException(e);
      }

  }

  public static <T> Builder<T> builder(HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper) {
        return new Builder<>(httpClient, jsonFactory, objectMapper);
    }

  public static class Builder<T> extends ReactiveFeign.Builder<T> {

      protected HttpClient httpClient;
      protected JsonFactory jsonFactory;
      private ObjectMapper objectMapper;
      protected ReactiveOptions options;

      protected Builder(HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper) {
          setHttpClient(httpClient, jsonFactory, objectMapper);
          this.jsonFactory = jsonFactory;
          this.objectMapper = objectMapper;
      }

      @Override
      public Builder<T> options(ReactiveOptions options) {
          if (options.getConnectTimeoutMillis() != null) {
              httpClient.setConnectTimeout(options.getConnectTimeoutMillis());
          }
          if (options.getReadTimeoutMillis() != null) {
              setHttpClient(httpClient, jsonFactory, objectMapper);
          }
          this.options = options;
          return this;
      }

      protected void setHttpClient(HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper){
          this.httpClient = httpClient;
          clientFactory(methodMetadata -> {
              JettyReactiveHttpClient jettyClient = JettyReactiveHttpClient.jettyClient(methodMetadata, httpClient, jsonFactory, objectMapper);
              if (options != null && options.getReadTimeoutMillis() != null) {
                  jettyClient.setRequestTimeout(options.getReadTimeoutMillis());
              }
              return jettyClient;
          });
      }
  }
}


