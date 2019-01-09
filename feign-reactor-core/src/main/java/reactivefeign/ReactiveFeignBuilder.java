package reactivefeign;

import feign.*;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.methodhandler.ReactiveMethodHandlerFactory;
import reactivefeign.publisher.PublisherClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReactiveFeignBuilder<T> {

    /**
     * Sets contract. Provided contract will be wrapped in {@link ReactiveContract}
     *
     * @param contract contract.
     * @return this builder
     */
    ReactiveFeignBuilder<T> contract(final Contract contract);

    /**
     * Set request interceptor that may modify request before being called
     * @param requestInterceptor
     * @return
     */
    ReactiveFeignBuilder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor);

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
     * The most common way to introduce custom logic on handling http response
     *
     * @param responseMapper
     * @return
     */
    ReactiveFeignBuilder<T> responseMapper(BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> responseMapper);

    ReactiveFeignBuilder<T> retryWhen(ReactiveRetryPolicy retryPolicy);

    ReactiveFeignBuilder<T> options(ReactiveOptions reactiveOptions);

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

    default MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory reactiveClientFactory) {
        return new ReactiveMethodHandlerFactory(reactiveClientFactory);
    }

    PublisherClientFactory buildReactiveClientFactory();

}
