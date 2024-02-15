package reactivefeign.rx3.client.statushandler;

import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactor.core.publisher.Mono;

import static reactor.adapter.rxjava.RxJava3Adapter.singleToMono;

public class Rx3ReactiveStatusHandler implements ReactiveStatusHandler {

	private final Rx3StatusHandler statusHandler;

	public Rx3ReactiveStatusHandler(Rx3StatusHandler statusHandler) {
		this.statusHandler = statusHandler;
	}

	@Override
	public boolean shouldHandle(int status) {
		return statusHandler.shouldHandle(status);
	}

	@Override
	public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse response) {
		return singleToMono(statusHandler.decode(methodKey, response));
	}
}
