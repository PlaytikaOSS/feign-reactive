package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;
import reactor.core.publisher.Mono;

import java.util.Map;

import static reactivefeign.benchmarks.RealRequestBenchmarks.PATH_WITH_PAYLOAD;

@Headers("Accept: application/json")
interface FeignReactorTestInterface {

  @RequestLine("GET /")
  Mono<String> justGet();

  @RequestLine("POST "+PATH_WITH_PAYLOAD)
  Mono<Map<String, Object>> postWithPayload(Mono<Map<String, Object>> payload);

}
