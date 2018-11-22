package reactivefeign.benchmarks;

import com.github.tomakehurst.wiremock.WireMockServer;
import feign.Feign;
import org.eclipse.jetty.client.HttpClient;
import org.openjdk.jmh.annotations.*;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.webclient.WebReactiveFeign;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Measurement(iterations = 10, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
abstract public class RealRequestBenchmarks {

  private WireMockServer server;

  protected FeignReactorTestInterface webClientFeign;

  protected HttpClient jettyHttpClient;
  protected FeignReactorTestInterface jettyFeign;

  protected FeignTestInterface feign;

  @Setup
  public void setup() throws Exception {

      server = new WireMockServer(wireMockConfig()
              .dynamicPort()
              .asynchronousResponseEnabled(true));

      server.stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("testBody")));

    server.start();

    int serverPort = server.port();
    webClientFeign = WebReactiveFeign.<FeignReactorTestInterface>builder()
        .target(FeignReactorTestInterface.class, "http://localhost:" + serverPort);

    jettyHttpClient = new HttpClient();
    jettyHttpClient.start();
    jettyFeign = JettyReactiveFeign.<FeignReactorTestInterface>builder(jettyHttpClient)
            .target(FeignReactorTestInterface.class, "http://localhost:" + serverPort);

    feign = Feign.builder().target(FeignTestInterface.class, "http://localhost:" + serverPort);
  }

  @TearDown
  public void tearDown() throws Exception {
    server.shutdown();
    jettyHttpClient.stop();
  }


}
