package reactivefeign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.FeignException;
import feign.InvocationHandlerFactory;
import feign.Target;
import reactivefeign.client.ReactiveErrorMapper;
import reactivefeign.client.ReactiveHttpExchangeFilterFunction;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponseMapper;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveFeignBuilder<T> {

    /**
     * Sets contract. Provided contract will be wrapped in {@link ReactiveContract}
     *
     * @param contract contract.
     * @return this builder
     */
    ReactiveFeignBuilder<T> contract(final Contract contract);

    /**
     * Set exchangeFilterFunction that may modify request before being called and response
     * @param exchangeFilterFunction
     * @return
     */
    ReactiveFeignBuilder<T> addExchangeFilterFunction(ReactiveHttpExchangeFilterFunction exchangeFilterFunction);

    /**
     * Set request interceptor that may modify request before being called
     * @param requestInterceptor
     * @return
     */
    ReactiveFeignBuilder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor);

    /**
     * May be used to collect request execution metrics
     * @param loggerListener
     * @return
     */
    ReactiveFeignBuilder<T> addLoggerListener(ReactiveLoggerListener loggerListener);

    /**
     * This flag indicates that the reactive feign client should process responses with 404 status,
     * specifically returning empty {@link Mono} or {@link Flux} instead of throwing
     * {@link FeignException}.
     * <p>
     * <p>
     * This flag only works with 404, as opposed to all or arbitrary status codes. This was an
     * explicit decision: 404 - empty is safe, common and doesn't complicate redirection, retry or
     * fallback policy.
     *
     * @return this builder
     */
    ReactiveFeignBuilder<T> decode404();

    /**
     * Allows to customize response status processing
     * @param statusHandler
     * @return
     */
    ReactiveFeignBuilder<T> statusHandler(ReactiveStatusHandler statusHandler);

    /**
     * Allows to add error mapper
     * @param errorMapper
     * @return
     */
    ReactiveFeignBuilder<T> errorMapper(ReactiveErrorMapper errorMapper);

    /**
     * The most common way to introduce custom json serialisation
     *
     * @param objectMapper
     * @return
     */
    ReactiveFeignBuilder<T> objectMapper(ObjectMapper objectMapper);

    /**
     * The most common way to introduce custom logic on handling http response
     *
     * @param responseMapper
     * @return
     */
    ReactiveFeignBuilder<T> responseMapper(ReactiveHttpResponseMapper<?> responseMapper);

    ReactiveFeignBuilder<T> retryWhen(ReactiveRetryPolicy retryPolicy);

    ReactiveFeignBuilder<T> options(ReactiveOptions reactiveOptions);

    ReactiveFeignBuilder<T> fallback(T fallback);

    ReactiveFeignBuilder<T> fallbackFactory(FallbackFactory<T> fallbackFactory);

    /**
     * Defines target and builds client.
     *
     * @param apiType API interface
     * @param url base URL
     * @return built client
     */
    default T target(final Class<T> apiType, final String url) {
        return target(new Target.HardCodedTarget<>(apiType, url));
    }

    default T target(final Class<T> apiType, final String name, final String url) {
        if(name.equals(url)){
            throw new IllegalArgumentException(String.format("Name is equal to url: name=[%s], url=[%s]", name, url));
        }
        if(!url.contains(name)){
            throw new IllegalArgumentException(String.format("Name should be part of url: name=[%s], url=[%s]", name, url));
        }
        return target(new Target.HardCodedTarget<>(apiType, name, url));
    }

    /**
     * Defines target and builds client.
     *
     * @param target target instance
     * @return built client
     */
    default T target(final Target<T> target) {
        return build().newInstance(target);
    }

    default ReactiveFeign build() {
        return new ReactiveFeign(contract(),
                buildReactiveMethodHandlerFactory(buildReactiveClientFactory()),
                invocationHandlerFactory());
    }

    Contract contract();

    default InvocationHandlerFactory invocationHandlerFactory(){
        return new ReactiveInvocationHandler.Factory();
    }

    MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory reactiveClientFactory);

    PublisherClientFactory buildReactiveClientFactory();

}
