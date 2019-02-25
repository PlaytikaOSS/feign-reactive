package reactivefeign.utils;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyInitializedTest {

    @Test
    public void shouldLazilyInitialize() throws ExecutionException, InterruptedException {

        AtomicInteger supplierCallNumber = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(2);

        LazyInitialized<Integer> cache = new LazyInitialized<>(() -> {
            try {
                countDownLatch.countDown();
                countDownLatch.await();
                return supplierCallNumber.incrementAndGet();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(cache::get);
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(cache::get);

        CompletableFuture.allOf(future1, future2).get();

        assertThat(supplierCallNumber.get()).isEqualTo(2);
        assertThat(future1.get()).isEqualTo(future2.get());

        assertThat(future1.get()).isEqualTo(cache.get());
        assertThat(supplierCallNumber.get()).isEqualTo(2);
    }

}
