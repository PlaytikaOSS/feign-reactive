package reactivefeign.methodhandler;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class FluxMethodHandler implements MethodHandler {

	private final MethodHandler methodHandler;

	public FluxMethodHandler(MethodHandler methodHandler) {
		this.methodHandler = methodHandler;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<Object> invoke(final Object[] argv) {
		try {
			return Flux.from((Publisher)methodHandler.invoke(argv));
		} catch (Throwable throwable) {
			return Flux.error(throwable);
		}
	}

}
