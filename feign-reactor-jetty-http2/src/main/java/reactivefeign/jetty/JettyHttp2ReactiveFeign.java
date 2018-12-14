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
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;

/**
 * Reactive Jetty Http/2 client based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public class JettyHttp2ReactiveFeign {

    public static <T> JettyReactiveFeign.Builder<T> builder() {
        try {
            HTTP2Client h2Client = new HTTP2Client();
            h2Client.setSelectors(1);
            HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

            HttpClient httpClient = new HttpClient(transport, null);
            httpClient.start();
            return builder(httpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> JettyReactiveFeign.Builder<T> builder(HttpClient httpClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return builder(httpClient, new JsonFactory(), objectMapper);
    }

    public static <T> JettyReactiveFeign.Builder<T> builder(HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper) {
        if(!(httpClient.getTransport() instanceof HttpClientTransportOverHTTP2)){
            throw new IllegalArgumentException("HttpClient should use HttpClientTransportOverHTTP2");
        }
        return new JettyReactiveFeign.Builder<>(httpClient, jsonFactory, objectMapper);
    }


}


