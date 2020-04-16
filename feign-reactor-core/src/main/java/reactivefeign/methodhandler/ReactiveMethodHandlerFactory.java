package reactivefeign.methodhandler;

import feign.MethodMetadata;
import feign.Target;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.ResponsePublisherHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static feign.Util.checkNotNull;
import static reactivefeign.utils.FeignUtils.isResponsePublisher;
import static reactivefeign.utils.FeignUtils.returnPublisherType;

public class ReactiveMethodHandlerFactory implements MethodHandlerFactory {

	private final PublisherClientFactory publisherClientFactory;
	private Target target;

	public ReactiveMethodHandlerFactory(final PublisherClientFactory publisherClientFactory) {
		this.publisherClientFactory = checkNotNull(publisherClientFactory, "client must not be null");
	}

	@Override
	public void target(Target target) {
		this.target = target;
		publisherClientFactory.target(target);
	}

	@Override
	public MethodHandler create(MethodMetadata metadata) {

		MethodHandler methodHandler = new PublisherClientMethodHandler(
				target, metadata, publisherClientFactory.create(metadata));

		if(isResponsePublisher(metadata.returnType())){
			return new MonoMethodHandler(methodHandler);
		}

		Type returnPublisherType = returnPublisherType(metadata);
		if(returnPublisherType == Mono.class){
			return new MonoMethodHandler(methodHandler);
		} else if(returnPublisherType == Flux.class) {
			return new FluxMethodHandler(methodHandler);
		} else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
		}
	}

	@Override
	public MethodHandler createDefault(Method method) {
		MethodHandler defaultMethodHandler = new DefaultMethodHandler(method);

		if(method.getReturnType() == Mono.class){
			return new MonoMethodHandler(defaultMethodHandler);
		} else if(method.getReturnType() == Flux.class) {
			return new FluxMethodHandler(defaultMethodHandler);
		} else {
			throw new IllegalArgumentException("Unknown returnPublisherType: " + method.getReturnType());
		}
	}
}
