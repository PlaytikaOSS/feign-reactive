package reactivefeign;

import java.util.function.Function;

public interface FallbackFactory<T> extends Function<Throwable, T> {
}
