package reactivefeign.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 3, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ParallelRequestBenchmarks extends RealRequestBenchmarks{

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

  //WITH PAYLOAD

  @Benchmark
  public void webClient() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> webClient
                            .method(HttpMethod.POST)
                            .uri(SERVER_URL+PATH_WITH_PAYLOAD)
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

  @Benchmark
  public void feignJettyH2c() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> jettyFeignH2c.postWithPayload(Mono.just(requestPayload)))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void feignJava11() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> java11Feign.postWithPayload(Mono.just(requestPayload)))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void feignJava11H2c() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> java11FeignH2c.postWithPayload(Mono.just(requestPayload)))
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  //used to run from IDE
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
            .jvmArgs("-Xms1024m", "-Xmx4024m")
            .include(".*" + ParallelRequestBenchmarks.class.getSimpleName() + ".*")
            //.addProfiler( StackProfiler.class )
            .build();

    new Runner(opt).run();
  }

}
