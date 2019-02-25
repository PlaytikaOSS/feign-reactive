package reactivefeign.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class LazyInitialized<V> {

    private final AtomicReference<V> cachedValue = new AtomicReference<>();
    private final Supplier<V> supplier;

    public LazyInitialized(Supplier<V> supplier) {
        this.supplier = supplier;
    }

    public V get(){
        V result = cachedValue.get();
        if (result == null) {
            result = supplier.get();
            if (!cachedValue.compareAndSet(null, result)) {
                return cachedValue.get();
            }
        }
        return result;
    }
}
