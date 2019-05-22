package reactivefeign.cloud;

import com.netflix.client.RetryHandler;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.Contract;
import feign.MethodMetadata;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactivefeign.FallbackFactory;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.cloud.methodhandler.HystrixMethodHandlerFactory;
import reactivefeign.cloud.publisher.RibbonPublisherClient;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static reactivefeign.methodhandler.PublisherClientMethodHandler.SUBSTITUTION_PATTERN;
import static reactivefeign.retry.FilteredReactiveRetryPolicy.notRetryOn;
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
        private FallbackFactory<T> fallbackFactory;
        private LoadBalancerCommandFactory loadBalancerCommandFactory = s -> null;

        protected Builder(ReactiveFeignBuilder<T> builder) {
            this.builder = builder;
        }

        public ReactiveFeignBuilder<T> getDelegate() {
            return builder;
        }

        public Builder<T> disableHystrix() {
            this.hystrixEnabled = false;
            return this;
        }

        public Builder<T> setHystrixCommandSetterFactory(SetterFactory commandSetterFactory) {
            this.commandSetterFactory = commandSetterFactory;
            return this;
        }

        public Builder<T> enableLoadBalancer(){
            return enableLoadBalancer(ReactiveFeignClientFactory.DEFAULT);
        }

        public Builder<T> enableLoadBalancer(ReactiveFeignClientFactory clientFactory){
            return setLoadBalancerCommandFactory(serviceName ->
                    LoadBalancerCommand.builder()
                            .withLoadBalancer(clientFactory.loadBalancer(serviceName))
                            .withClientConfig(clientFactory.clientConfig(serviceName))
                            .build());
        }

        public Builder<T> enableLoadBalancer(ReactiveFeignClientFactory clientFactory, RetryHandler retryHandler){
            if(retryHandler.getMaxRetriesOnSameServer() > 0){
                logger.warn("Use retryWhen(ReactiveRetryPolicy retryPolicy) " +
                        "as it allow to configure retry delays (backoff)");
            }
            return setLoadBalancerCommandFactory(serviceName ->
                    LoadBalancerCommand.builder()
                    .withLoadBalancer(clientFactory.loadBalancer(serviceName))
                    .withClientConfig(clientFactory.clientConfig(serviceName))
                    .withRetryHandler(retryHandler)
                    .build());
        }


        public Builder<T> setLoadBalancerCommandFactory(LoadBalancerCommandFactory loadBalancerCommandFactory) {
            this.loadBalancerCommandFactory = loadBalancerCommandFactory;
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
        public ReactiveFeignBuilder<T> addLoggerListener(ReactiveLoggerListener loggerListener) {
            builder = builder.addLoggerListener(loggerListener);
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
            builder =  builder.retryWhen(notRetryOn(retryPolicy, HystrixBadRequestException.class));
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
                    return new RibbonPublisherClient(loadBalancerCommandFactory, serviceName,
                            publisherClient, returnPublisherType(methodMetadata));
                }
            };
        }

        private String extractServiceName(String url){
            try {
                String substitutedUrl = url.replaceAll(SUBSTITUTION_PATTERN.toString(), "tempsubstitution");
                return new URI(substitutedUrl).getHost();
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
