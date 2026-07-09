package reactivefeign.methodhandler;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class FlowMethodHandler implements MethodHandler {

    private final MethodHandler methodHandler;

    public FlowMethodHandler(MethodHandler methodHandler) {
        this.methodHandler = methodHandler;
    }

    @Override
    public Flow<Object> invoke(final Object[] argv) {
        return ReactiveFlowKt.asFlow(invokeFlux(argv));
    }

    @SuppressWarnings("unchecked")
    protected Flux<Object> invokeFlux(final Object[] argv) {
        try {
            return Flux.from((Publisher) methodHandler.invoke(argv));
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
    }

}
