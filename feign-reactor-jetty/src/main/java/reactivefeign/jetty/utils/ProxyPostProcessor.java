package reactivefeign.jetty.utils;

import org.eclipse.jetty.reactive.client.internal.AbstractSingleProcessor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.function.BiConsumer;

public class ProxyPostProcessor<I> extends AbstractSingleProcessor<I, I>{

	private final Publisher<I> publisher;
	private final BiConsumer<I, Throwable> postProcessor;

	private ProxyPostProcessor(Publisher<I> publisher, BiConsumer<I, Throwable> postProcessor) {
		this.publisher = publisher;
		this.postProcessor = postProcessor;
	}

	@Override
	public void onNext(I i) {
		try {
			downStreamOnNext(i);
			postProcessor.accept(i, null);
		} catch (Throwable err) {
			postProcessor.accept(i, err);
		}
	}

	@Override
	public void subscribe(Subscriber<? super I> s) {
		publisher.subscribe(this);
		super.subscribe(s);
	}

	public static <I> Publisher<I> postProcess(Publisher<I> publisher, BiConsumer<I, Throwable> postProcessor){
		return new ProxyPostProcessor<>(publisher, postProcessor);
	}
}
