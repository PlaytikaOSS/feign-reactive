package reactivefeign.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;

public class RealSingleRequestBenchmarks extends RealRequestBenchmarks{

  /**
   * How fast can we execute get commands synchronously in parallel using Feign?
   */
  @Benchmark
  public String query_feign() {
    return feign.justGet();
  }

  /**
   * How fast can we execute get commands in parallel using reactive web client based Feign?
   */
  @Benchmark
  public String query_webClientFeign() {
    return webClientFeign.justGet().block();
  }

  /**
   * How fast can we execute get commands in parallel using reactive Jetty based Feign?
   */
  @Benchmark
  public String query_jettyFeign() {
    return jettyFeign.justGet().block();
  }

}
