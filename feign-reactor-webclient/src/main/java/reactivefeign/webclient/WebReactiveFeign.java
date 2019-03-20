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
package reactivefeign.webclient;

import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;

import static reactivefeign.webclient.client.WebReactiveHttpClient.webClient;

/**
 * {@link WebClient} based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public class WebReactiveFeign {

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Builder<T> builder(WebClientFeignCustomizer webClientCustomizer) {
        return new Builder<>(webClientCustomizer);
    }

    public static class Builder<T> extends ReactiveFeign.Builder<T> {

        protected CustomizableWebClientBuilder webClientBuilder = new CustomizableWebClientBuilder();

        protected Builder() {
            updateClientFactory();
        }

        protected Builder(WebClientFeignCustomizer webClientCustomizer) {
            webClientCustomizer.accept(webClientBuilder);
            updateClientFactory();
        }

        @Override
        public Builder<T> options(ReactiveOptions options) {
            webClientBuilder.setWebOptions((WebReactiveOptions)options);
            updateClientFactory();
            return this;
        }

        protected void updateClientFactory(){
            clientFactory(methodMetadata -> webClient(methodMetadata, webClientBuilder.build()));
        }
    }


}


