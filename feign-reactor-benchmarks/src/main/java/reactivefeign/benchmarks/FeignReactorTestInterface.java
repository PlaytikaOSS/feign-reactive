package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;
import feign.Response;
import reactor.core.publisher.Mono;

import java.util.Map;

@Headers("Accept: application/json")
interface FeignReactorTestInterface {

  @RequestLine("GET /")
  Mono<String> justGet();

  @RequestLine("POST /postWithPayload")
  Mono<Map<String, Object>> postWithPayload(Mono<Map<String, Object>> payload);

}
