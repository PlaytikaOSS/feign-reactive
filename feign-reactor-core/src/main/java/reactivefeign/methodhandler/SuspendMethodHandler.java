package reactivefeign.methodhandler;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactor.MonoKt;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Arrays;

public class SuspendMethodHandler implements MethodHandler {

    private final MethodHandler methodHandler;

    public SuspendMethodHandler(MethodHandler methodHandler) {
        this.methodHandler = methodHandler;
    }

    @Override
    public Object invoke(final Object[] argv) {
        Object[] args = Arrays.copyOf(argv, argv.length - 1);
        Continuation continuation = (Continuation) argv[argv.length - 1];
        return MonoKt.awaitSingleOrNull(invokeMono(args), continuation);
    }

    @SuppressWarnings("unchecked")
    public Mono<Object> invokeMono(final Object[] argv) {
        try {
            return Mono.from((Publisher) methodHandler.invoke(argv));
        } catch (Throwable throwable) {
            return Mono.error(throwable);
        }
    }
}
