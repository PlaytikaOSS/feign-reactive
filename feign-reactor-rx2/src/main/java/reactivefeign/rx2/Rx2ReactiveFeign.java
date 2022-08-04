package reactivefeign.rx2;

import feign.Contract;
import feign.MethodMetadata;
import io.reactivex.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.FluxPublisherHttpClient;
import reactivefeign.publisher.MonoPublisherHttpClient;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactivefeign.rx2.client.statushandler.Rx2ReactiveStatusHandler;
import reactivefeign.rx2.client.statushandler.Rx2StatusHandler;
import reactivefeign.rx2.methodhandler.Rx2MethodHandlerFactory;
import reactivefeign.webclient.WebClientFeignCustomizer;
import reactivefeign.webclient.WebReactiveFeign;
import reactivefeign.webclient.client.WebReactiveHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static reactivefeign.utils.FeignUtils.getBodyActualType;
import static reactivefeign.utils.FeignUtils.returnPublisherType;
import static reactivefeign.webclient.client.WebReactiveHttpClient.webReactiveHttpResponse;

/**
 * @author Sergii Karpenko
 */
public final class Rx2ReactiveFeign {

    private Rx2ReactiveFeign(){}

    public static <T> Builder<T> builder() {
        return new Builder<>(WebClient.builder());
    }

    public static <T> Builder<T> builder(WebClient.Builder webClientBuilder) {
        return new Builder<>(webClientBuilder);
    }

    public static <T> Builder<T> builder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
        return new Builder<>(webClientBuilder, webClientCustomizer);
    }

    public static class Builder<T> extends WebReactiveFeign.Builder<T> {

        private BackpressureStrategy backpressureStrategy;

        protected Builder(WebClient.Builder webClientBuilder) {
            super(webClientBuilder);
        }

        protected Builder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
            super(webClientBuilder, webClientCustomizer);
        }

        /**
         * Used to convert {@link Observable}  into {@link Flux}
         * @param backpressureStrategy
         */
        public void setBackpressureStrategy(BackpressureStrategy backpressureStrategy) {
            this.backpressureStrategy = backpressureStrategy;
        }

        @Override
        public MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory publisherClientFactory) {
            return new Rx2MethodHandlerFactory(publisherClientFactory, backpressureStrategy);
        }

        @Override
        public Builder<T> contract(final Contract contract) {
            this.contract = new Rx2Contract(contract);
            return this;
        }

        @Override
        public Builder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
            super.addRequestInterceptor(requestInterceptor);
            return this;
        }

        @Override
        public Builder<T> decode404() {
            super.decode404();
            return this;
        }

        public Builder<T> statusHandler(Rx2StatusHandler statusHandler) {
            super.statusHandler(new Rx2ReactiveStatusHandler(statusHandler));
            return this;
        }

        @Override
        public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy){
            super.retryWhen(retryPolicy);
            return this;
        }

        @Override
        public Builder<T> options(final ReactiveOptions options) {
            super.options(options);
            return this;
        }

        protected PublisherHttpClient toPublisher(ReactiveHttpClient reactiveHttpClient, MethodMetadata methodMetadata){
            Type returnType = returnPublisherType(methodMetadata);
            if(returnType == Single.class || returnType == Maybe.class){
                return new MonoPublisherHttpClient(reactiveHttpClient);
            } else if(returnType == Flowable.class || returnType == Observable.class){
                return new FluxPublisherHttpClient(reactiveHttpClient);
            } else {
                throw new IllegalArgumentException("Unknown returnType: " + returnType);
            }
        }

        @Override
        protected ReactiveHttpClientFactory clientFactory(){
            this.webClientBuilder.clientConnector(clientConnector());

            if(webClientCustomizer != null){
                webClientCustomizer.accept(webClientBuilder);
            }

            return methodMetadata -> webClient(methodMetadata, webClientBuilder.build());
        }

        public WebReactiveHttpClient webClient(MethodMetadata methodMetadata, WebClient webClient) {

            final Type returnType = methodMetadata.returnType();
            Type returnPublisherType = ((ParameterizedType) returnType).getRawType();
            ParameterizedTypeReference<Object> returnActualType = ParameterizedTypeReference.forType(
                    resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
            ParameterizedTypeReference<Object> bodyActualType = ofNullable(
                    getBodyActualType(methodMetadata.bodyType()))
                    .map(type -> ParameterizedTypeReference.forType(type))
                    .orElse(null);

            return new WebReactiveHttpClient(webClient, bodyActualType,
                    webReactiveHttpResponse(rx2ToReactor(returnPublisherType), returnActualType),
                    errorMapper());
        }

        private static Class rx2ToReactor(Type type){
            if(type == Flowable.class){
                return Flux.class;
            } else if(type == Observable.class){
                return Flux.class;
            } else if(type == Single.class){
                return Mono.class;
            } else if(type == Maybe.class){
                return Mono.class;
            } else {
                throw new IllegalArgumentException("Unexpected type="+type);
            }
        }
    }

}
