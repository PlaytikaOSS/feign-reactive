package reactivefeign.cloud;

import com.netflix.client.ClientFactory;
import com.netflix.client.RetryHandler;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.Contract;
import feign.MethodMetadata;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;
import reactivefeign.ReactiveRetryPolicy;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.cloud.methodhandler.HystrixMethodHandlerFactory;
import reactivefeign.cloud.publisher.RibbonPublisherClient;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static reactivefeign.utils.FeignUtils.returnPublisherType;

/**
 * Allows to specify ribbon {@link LoadBalancerCommand}
 * and HystrixObservableCommand.Setter with fallback factory.
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
        private boolean hystrixEnabled = true;
        private SetterFactory commandSetterFactory = new DefaultSetterFactory();
        private Function<Throwable, ? extends T> fallbackFactory;
        private Function<String, LoadBalancerCommand<Object>> loadBalancerCommandFactory = s -> null;

        protected Builder(ReactiveFeignBuilder<T> builder) {
            this.builder = builder;
        }

        public Builder<T> disableHystrix() {
            this.hystrixEnabled = false;
            return this;
        }

        public Builder<T> setHystrixCommandSetterFactory(SetterFactory commandSetterFactory) {
            this.commandSetterFactory = commandSetterFactory;
            return this;
        }

        public Builder<T> setFallback(T fallback) {
            return setFallbackFactory(throwable -> fallback);
        }

        public Builder<T> setFallbackFactory(Function<Throwable, ? extends T> fallbackFactory) {
            this.fallbackFactory = fallbackFactory;
            return this;
        }

        public Builder<T> enableLoadBalancer(){
            return setLoadBalancerCommandFactory(serviceName ->
                    LoadBalancerCommand.builder()
                            .withLoadBalancer(ClientFactory.getNamedLoadBalancer(serviceName))
                            .build());
        }

        public Builder<T> enableLoadBalancer(RetryHandler retryHandler){
            if(retryHandler.getMaxRetriesOnSameServer() > 0){
                logger.warn("Use retryWhen(ReactiveRetryPolicy retryPolicy) " +
                        "as it allow to configure retry delays (backoff)");
            }
            return setLoadBalancerCommandFactory(serviceName ->
                    LoadBalancerCommand.builder()
                    .withLoadBalancer(ClientFactory.getNamedLoadBalancer(serviceName))
                    .withRetryHandler(retryHandler)
                    .build());
        }


        public Builder<T> setLoadBalancerCommandFactory(
                Function<String, LoadBalancerCommand<Object>> loadBalancerCommandFactory) {
            this.loadBalancerCommandFactory = loadBalancerCommandFactory;
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> contract(Contract contract) {
            builder = builder.contract(contract);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> options(ReactiveOptions options) {
            builder = builder.options(options);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
            builder = builder.addRequestInterceptor(requestInterceptor);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> decode404() {
            builder = builder.decode404();
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> statusHandler(ReactiveStatusHandler statusHandler) {
            builder = builder.statusHandler(statusHandler);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> responseMapper(BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> responseMapper) {
            builder =  builder.responseMapper(responseMapper);
            return this;
        }

        @Override
        public ReactiveFeignBuilder<T> retryWhen(ReactiveRetryPolicy retryPolicy) {
            builder =  builder.retryWhen(retryPolicy);
            return this;
        }

        @Override
        public Contract contract() {
            return builder.contract();
        }

        @Override
        public MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory reactiveClientFactory) {
            MethodHandlerFactory methodHandlerFactory = builder.buildReactiveMethodHandlerFactory(reactiveClientFactory);
            return hystrixEnabled
                    ? new HystrixMethodHandlerFactory(
					methodHandlerFactory,
                    commandSetterFactory,
                    (Function<Throwable, Object>) fallbackFactory)
                    : methodHandlerFactory;
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
                    String serviceName = extractServiceName(target.url());
                    return new RibbonPublisherClient(loadBalancerCommandFactory.apply(serviceName),
                            publisherClient, returnPublisherType(methodMetadata));
                }
            };
        }

        private String extractServiceName(String url){
            try {
                return new URI(url).getHost();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Can't extract service name from url:"+url, e);
            }
        }

    }

    public interface SetterFactory {
        HystrixObservableCommand.Setter create(Target<?> target, MethodMetadata methodMetadata);
    }

    public static class DefaultSetterFactory implements SetterFactory {
        @Override
        public HystrixObservableCommand.Setter create(Target<?> target, MethodMetadata methodMetadata) {
            String groupKey = target.name();
            String commandKey = methodMetadata.configKey();
            return HystrixObservableCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
        }
    }

}
