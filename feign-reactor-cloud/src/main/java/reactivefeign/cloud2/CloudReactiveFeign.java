package reactivefeign.cloud2;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.MethodMetadata;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.FallbackFactory;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveErrorMapper;
import reactivefeign.client.ReactiveHttpExchangeFilterFunction;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponseMapper;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.cloud2.methodhandler.CircuitBreakerMethodHandlerFactory;
import reactivefeign.cloud2.publisher.LoadBalancerPublisherClient;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;

import java.util.function.Function;

import static reactivefeign.ReactiveFeign.Builder.retry;

/**
 * Allows to specify load balancer {@link ReactiveLoadBalancer.Factory<ServiceInstance>}
 * and ReactiveCircuitBreakerFactory with fallback factory.
 *
 * @author Sergii Karpenko
 */
public class CloudReactiveFeign {

    private static final Logger logger = LoggerFactory.getLogger(CloudReactiveFeign.class);

    public static <T> Builder<T> builder(ReactiveFeignBuilder<T> builder) {
        return new Builder<>(builder);
    }

    public static class Builder<T> implements ReactiveFeignBuilder<T> {

        private ReactiveFeignBuilder<T> builder;
        private ReactiveFeignCircuitBreakerFactory circuitBreakerFactory;
        private FallbackFactory<T> fallbackFactory;
        private ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;
        private ReactiveRetryPolicy retryOnNextPolicy;

        protected Builder(ReactiveFeignBuilder<T> builder) {
            this.builder = builder;
        }

        public Builder<T> enableCircuitBreaker(ReactiveFeignCircuitBreakerFactory circuitBreakerFactory) {
            this.circuitBreakerFactory = circuitBreakerFactory;
            return this;
        }

        public Builder<T> enableLoadBalancer(
                ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory){
            this.loadBalancerFactory = loadBalancerFactory;
            return this;
        }

        public Builder<T> retryOnSame(ReactiveRetryPolicy retryOnSamePolicy){
            if(this.loadBalancerFactory == null){
                throw new IllegalArgumentException("loadBalancerFactory should be specified");
            }
            retryWhen(retryOnSamePolicy);
            return this;
        }

        public Builder<T> retryOnNext(ReactiveRetryPolicy retryOnNextPolicy){
            if(this.loadBalancerFactory == null){
                throw new IllegalArgumentException("loadBalancerFactory should be specified");
            }
            this.retryOnNextPolicy = retryOnNextPolicy;
            return this;
        }

        @Override
        public Builder<T> fallback(T fallback) {
            return fallbackFactory(throwable -> fallback);
        }

        @Override
        public Builder<T> fallbackFactory(FallbackFactory<T> fallbackFactory) {
            this.fallbackFactory = fallbackFactory;
            return this;
        }

        @Override
        public Builder<T> contract(Contract contract) {
            builder = builder.contract(contract);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> addExchangeFilterFunction(ReactiveHttpExchangeFilterFunction exchangeFilterFunction) {
            builder = builder.addExchangeFilterFunction(exchangeFilterFunction);
            return this;
        }

        @Override
        public Builder<T> options(ReactiveOptions options) {
            builder = builder.options(options);
            return this;
        }

        @Override
        public Builder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
            builder = builder.addRequestInterceptor(requestInterceptor);
            return this;
        }

        @Override
        public Builder<T> addLoggerListener(ReactiveLoggerListener loggerListener) {
            builder = builder.addLoggerListener(loggerListener);
            return this;
        }

        @Override
        public Builder<T> decode404() {
            builder = builder.decode404();
            return this;
        }

        @Override
        public Builder<T> statusHandler(ReactiveStatusHandler statusHandler) {
            builder = builder.statusHandler(statusHandler);
            return this;
        }

        @Override
        public Builder<T> errorMapper(ReactiveErrorMapper errorMapper) {
            builder = builder.errorMapper(errorMapper);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> objectMapper(ObjectMapper objectMapper) {
            builder = builder.objectMapper(objectMapper);
            return this;
        }

        @Override
        public Builder<T> responseMapper(ReactiveHttpResponseMapper responseMapper) {
            builder =  builder.responseMapper(responseMapper);
            return this;
        }

        @Override
        public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy) {
            builder =  builder.retryWhen(retryPolicy);
            return this;
        }

        @Override
        public Contract contract() {
            return builder.contract();
        }

        @Override
        public MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory reactiveClientFactory) {
            if(circuitBreakerFactory == null){
                builder.fallbackFactory(fallbackFactory);
                return builder.buildReactiveMethodHandlerFactory(reactiveClientFactory);
            } else {
                return new CircuitBreakerMethodHandlerFactory(
                        builder.buildReactiveMethodHandlerFactory(reactiveClientFactory),
                        circuitBreakerFactory,
                        (Function<Throwable, Object>) fallbackFactory);
            }
        }

        @Override
        public PublisherClientFactory buildReactiveClientFactory() {
            PublisherClientFactory publisherClientFactory = builder.buildReactiveClientFactory();
            return new PublisherClientFactory(){

                private Target target;

                @Override
                public void target(Target target) {
                    this.target = target;
                    publisherClientFactory.target(target);
                }

                @Override
                public PublisherHttpClient create(MethodMetadata methodMetadata) {
                    PublisherHttpClient publisherClient = publisherClientFactory.create(methodMetadata);
                    if(!target.name().equals(target.url()) && loadBalancerFactory != null){
                        publisherClient = new LoadBalancerPublisherClient(
                                loadBalancerFactory.getInstance(target.name()), publisherClient);
                        if(retryOnNextPolicy != null){
                            publisherClient = retry(publisherClient, methodMetadata, retryOnNextPolicy);
                        }
                        return publisherClient;
                    } else {
                        if(retryOnNextPolicy != null){
                            logger.warn("retryOnNextPolicy will be ignored " +
                                    "as loadBalancerFactory is not configured for {} reactive feign client", target.name());
                        }
                        return publisherClient;
                    }

                }
            };
        }
    }

}
