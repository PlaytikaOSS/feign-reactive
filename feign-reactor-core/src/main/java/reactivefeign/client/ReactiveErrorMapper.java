package reactivefeign.client;

import java.util.function.BiFunction;

public interface ReactiveErrorMapper extends BiFunction<ReactiveHttpRequest, Throwable, Throwable> {
}
