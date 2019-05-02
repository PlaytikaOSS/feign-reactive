package reactivefeign.rx2.methodhandler;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.reactivestreams.Publisher;
import reactivefeign.methodhandler.MethodHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

import static reactor.adapter.rxjava.RxJava2Adapter.*;

public class Rx2MethodHandler implements MethodHandler {

	private final MethodHandler methodHandler;
	private final Type returnPublisherType;

	public Rx2MethodHandler(MethodHandler methodHandler, Type returnPublisherType) {
		this.methodHandler = methodHandler;
		this.returnPublisherType = returnPublisherType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object invoke(final Object[] argv) {
		try {
			Publisher<Object> publisher = (Publisher<Object>)methodHandler.invoke(argv);
			if(returnPublisherType == Flowable.class){
				return fluxToFlowable((Flux<Object>) publisher);
			} else if(returnPublisherType == Observable.class){
				return fluxToObservable((Flux<Object>) publisher);
			} else if(returnPublisherType == Single.class){
				return monoToSingle((Mono<Object>) publisher);
			} else if(returnPublisherType == Maybe.class){
				return monoToMaybe((Mono<Object>) publisher);
			} else {
				throw new IllegalArgumentException("Unexpected returnPublisherType="+returnPublisherType.getClass());
			}
		} catch (Throwable throwable) {
			if(returnPublisherType == Flowable.class){
				return Flowable.error(throwable);
			} else if(returnPublisherType == Observable.class){
				return Observable.error(throwable);
			} else if(returnPublisherType == Single.class){
				return Single.error(throwable);
			} else if(returnPublisherType == Maybe.class){
				return Maybe.error(throwable);
			} else {
				throw new IllegalArgumentException("Unexpected returnPublisherType="+returnPublisherType.getClass());
			}
		}
	}
}
