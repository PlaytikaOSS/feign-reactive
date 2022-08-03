package reactivefeign.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;


public interface ReactiveHttpExchangeFilterFunction<P extends Publisher<?>> {

    Mono<ReactiveHttpResponse<P>> filter(
            ReactiveHttpRequest request,
            ReactiveHttpClient<P> exchangeFunction);

    default ReactiveHttpExchangeFilterFunction<P> then(
            ReactiveHttpExchangeFilterFunction<P> afterFilter) {
        return (request, next) -> filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
    }

    default ReactiveHttpClient<P> filter(ReactiveHttpClient<P> exchangeFunction){
        return request -> filter(request, exchangeFunction);
    }

    static <P extends Publisher<?>> ReactiveHttpExchangeFilterFunction<P> ofRequestProcessor(
            Function<ReactiveHttpRequest, Mono<ReactiveHttpRequest>> processor) {
        return (request, next) -> processor.apply(request).flatMap(next::executeRequest);
    }

    static <P extends Publisher<?>> ReactiveHttpExchangeFilterFunction<P> ofResponseProcessor(
            Function<ReactiveHttpResponse<P>, Mono<ReactiveHttpResponse<P>>> processor) {
        return (request, next) -> next.executeRequest(request).flatMap(processor);
    }

    static <P extends Publisher<?>> ReactiveHttpExchangeFilterFunction<P> ofErrorMapper(
            ReactiveErrorMapper errorMapper) {
        return (request, next) -> next.executeRequest(request)
                .onErrorMap(throwable -> errorMapper.apply(request, throwable));
    }

}
