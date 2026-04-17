package reactivefeign.webclient.client;

import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;

/**
 * Wraps a {@link ReactiveHttpResponse} so that terminal events on the body publisher
 * (complete, error or cancel) signal a {@link Sinks.One Void gate}. This gate is used by
 * {@link WebReactiveHttpClient} to keep the underlying {@code WebClient.exchangeToMono}
 * subscription alive until the body has been consumed, which in Spring 7+ is required
 * to prevent premature release of the response body.
 */
class GatedReactiveHttpResponse<P extends Publisher<?>> implements ReactiveHttpResponse<P> {

    private final ReactiveHttpResponse<P> delegate;
    private final Sinks.One<Void> gate;

    GatedReactiveHttpResponse(ReactiveHttpResponse<P> delegate, Sinks.One<Void> gate) {
        this.delegate = delegate;
        this.gate = gate;
    }

    private void release() {
        gate.tryEmitEmpty();
    }

    @Override
    public ReactiveHttpRequest request() {
        return delegate.request();
    }

    @Override
    public int status() {
        return delegate.status();
    }

    @Override
    public Map<String, List<String>> headers() {
        return delegate.headers();
    }

    @Override
    @SuppressWarnings("unchecked")
    public P body() {
        P body = delegate.body();
        if (body instanceof Mono<?> mono) {
            return (P) mono.doFinally(st -> release());
        } else if (body instanceof Flux<?> flux) {
            return (P) flux.doFinally(st -> release());
        }
        return (P) Flux.from(body).doFinally(st -> release());
    }

    @Override
    public Mono<Void> releaseBody() {
        return delegate.releaseBody().doFinally(st -> release());
    }

    @Override
    public Mono<byte[]> bodyData() {
        return delegate.bodyData().doFinally(st -> release());
    }
}
