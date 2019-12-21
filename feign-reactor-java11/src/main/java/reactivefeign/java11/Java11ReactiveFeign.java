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
package reactivefeign.java11;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;
import reactivefeign.java11.client.Java11ReactiveHttpClientFactory;

import java.net.http.HttpClient;
import java.time.Duration;

import static reactivefeign.ReactiveOptions.useHttp2;

/**
 * Reactive Java 11 client based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public final class Java11ReactiveFeign {

    private Java11ReactiveFeign(){}

    public static <T> Builder<T> builder() {
        try {
            return builder(HttpClient.newBuilder());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Builder<T> builder(HttpClient.Builder httpClientBuilder) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return builder(httpClientBuilder, new JsonFactory(), objectMapper);
    }

    public static <T> Builder<T> builder(HttpClientFeignCustomizer clientFeignCustomizer) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        clientFeignCustomizer.accept(httpClientBuilder);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return builder(httpClientBuilder, new JsonFactory(), objectMapper);
    }

    public static <T> Builder<T> builder(HttpClient.Builder httpClientBuilder, JsonFactory jsonFactory, ObjectMapper objectMapper) {
        return new Builder<>(httpClientBuilder, jsonFactory, objectMapper);
    }

    public static class Builder<T> extends ReactiveFeign.Builder<T> {

        protected HttpClient.Builder httpClientBuilder;
        protected JsonFactory jsonFactory;
        private ObjectMapper objectMapper;
        protected Java11ReactiveOptions options;

        protected Builder(HttpClient.Builder httpClientBuilder, JsonFactory jsonFactory, ObjectMapper objectMapper) {
            setHttpClient(httpClientBuilder, jsonFactory, objectMapper);
            this.jsonFactory = jsonFactory;
            this.objectMapper = objectMapper;
        }

        @Override
        public Builder<T> options(ReactiveOptions options) {
            this.options = (Java11ReactiveOptions)options;

            if (this.options.getConnectTimeoutMillis() != null) {
                this.httpClientBuilder = httpClientBuilder.connectTimeout(
                        Duration.ofMillis(options.getConnectTimeoutMillis()));
            }

            setHttpClient(httpClientBuilder, jsonFactory, objectMapper);

            return this;
        }

        protected void setHttpClient(HttpClient.Builder httpClientBuilder, JsonFactory jsonFactory, ObjectMapper objectMapper){
            this.httpClientBuilder = httpClientBuilder;

            this.httpClientBuilder = httpClientBuilder.version(
                    useHttp2(this.options) ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1);

            HttpClient httpClient = httpClientBuilder.build();

            clientFactory(new Java11ReactiveHttpClientFactory(httpClient, jsonFactory, objectMapper, options));
        }
    }
}


