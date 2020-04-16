package reactivefeign.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface ReactiveHttpResponseMapper<P extends Publisher<?>>
        extends Function<ReactiveHttpResponse<P>, Mono<ReactiveHttpResponse<P>>> {
}
