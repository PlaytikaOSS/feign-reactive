package reactivefeign.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 3, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class RealParallelRequestBenchmarks extends RealRequestBenchmarks{

  public static final int CALLS_NUMBER = 100;
  private ExecutorService executor;

  @Setup
  public void setup() throws Exception {
    super.setup();
    executor = Executors.newFixedThreadPool(CALLS_NUMBER);
  }

  @TearDown
  public void tearDown() throws Exception {
    super.tearDown();
    executor.shutdown();
  }

  //NO PAYLOAD

  @Benchmark
  public void feignEmptyPayload() throws ExecutionException, InterruptedException {

    CompletableFuture[] bonusesCompletableFutures = IntStream.range(0, CALLS_NUMBER)
            .mapToObj(runnable -> CompletableFuture.runAsync(() -> feign.justGet(), executor))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(bonusesCompletableFutures).get();
  }

  /**
   * How fast can we execute get commands synchronously using reactive web client based Feign?
   */
  @Benchmark
  public void webClientEmptyPayload() {

    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> webClientFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void jettyEmptyPayload() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> jettyFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  //WITH PAYLOAD

  @Benchmark
  public void webClient() {

    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> webClient
                            .method(HttpMethod.POST)
                            .uri(SERVER_URL+"/postWithPayload")
                            .body(Mono.just(requestPayload), Map.class)
                            .header(HttpHeaders.CONTENT_TYPE,"application/json")
                            .retrieve()
                            .bodyToMono(Map.class))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void feign() throws ExecutionException, InterruptedException {

    CompletableFuture[] bonusesCompletableFutures = IntStream.range(0, CALLS_NUMBER)
            .mapToObj(runnable -> CompletableFuture.runAsync(() -> feign.postWithPayload(requestPayload), executor))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(bonusesCompletableFutures).get();
  }

  /**
   * How fast can we execute get commands synchronously using reactive web client based Feign?
   */
  @Benchmark
  public void feignWebClient() {

    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> webClientFeign.postWithPayload(Mono.just(requestPayload)))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void feignJetty() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> jettyFeign.postWithPayload(Mono.just(requestPayload)))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  //used to run from IDE
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
            .include(".*" + RealParallelRequestBenchmarks.class.getSimpleName() + ".*")
            //.addProfiler( StackProfiler.class )
            .build();

    new Runner(opt).run();
  }

}
