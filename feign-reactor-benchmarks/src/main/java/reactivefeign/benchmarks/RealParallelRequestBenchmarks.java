package reactivefeign.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RealParallelRequestBenchmarks extends RealRequestBenchmarks{

  private ExecutorService executor;

  @Setup
  public void setup() throws Exception {
    super.setup();
    executor = Executors.newFixedThreadPool(100);
  }

  @TearDown
  public void tearDown() throws Exception {
    super.tearDown();
    executor.shutdown();
  }


  @Benchmark
  public void query_feign_parallel() throws ExecutionException, InterruptedException {

    CompletableFuture[] bonusesCompletableFutures = IntStream.range(0, 100)
            .mapToObj(runnable -> CompletableFuture.runAsync(() -> feign.justGet(), executor))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(bonusesCompletableFutures).get();
  }


  /**
   * How fast can we execute get commands synchronously using reactive web client based Feign?
   */
  @Benchmark
  public void query_webClientFeign_parallel() {

    Mono.zip(IntStream.range(0, 100)
                    .mapToObj(i -> webClientFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }


  @Benchmark
  public void query_jettyFeign_parallel() {
    Mono.zip(IntStream.range(0, 100)
                    .mapToObj(i -> jettyFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

}
