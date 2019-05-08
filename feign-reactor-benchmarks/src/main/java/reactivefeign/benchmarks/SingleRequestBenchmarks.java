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

import java.util.concurrent.TimeUnit;

@Measurement(iterations = 10, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SingleRequestBenchmarks extends RealRequestBenchmarks{

    @Setup
    public void setup() throws Exception {
        super.setup();
    }

    @TearDown
    public void tearDown() throws Exception {
        super.tearDown();
    }

  /**
   * How fast can we execute get commands synchronously in parallel using Feign?
   */
  @Benchmark
  public String feign() {
    return feign.justGet();
  }

  /**
   * How fast can we execute get commands in parallel using reactive web client based Feign?
   */
  @Benchmark
  public String webClient() {
    return webClientFeign.justGet().block();
  }

  /**
   * How fast can we execute get commands in parallel using reactive Jetty based Feign?
   */
  @Benchmark
  public String jetty() {
    return jettyFeign.justGet().block();
  }

}
