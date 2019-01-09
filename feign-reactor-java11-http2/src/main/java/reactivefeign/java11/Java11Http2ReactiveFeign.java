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

import java.net.http.HttpClient;

/**
 * Reactive Java 11 over Http/2 client based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public class Java11Http2ReactiveFeign {

    public static <T> Java11ReactiveFeign.Builder<T> builder() {
        try {
            return builder(HttpClient.newBuilder());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Java11ReactiveFeign.Builder<T> builder(HttpClient.Builder httpClientBuilder) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return builder(httpClientBuilder, new JsonFactory(), objectMapper);
    }

    public static <T> Java11ReactiveFeign.Builder<T> builder(HttpClient.Builder httpClientBuilder, JsonFactory jsonFactory, ObjectMapper objectMapper) {
        Java11ReactiveFeign.Builder<T> builder = new Java11ReactiveFeign.Builder<>(httpClientBuilder, jsonFactory, objectMapper);
        builder.version(HttpClient.Version.HTTP_2);
        return builder;
    }


}


