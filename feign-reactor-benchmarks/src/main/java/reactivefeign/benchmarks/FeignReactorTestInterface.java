package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;
import feign.Response;
import reactor.core.publisher.Mono;

@Headers("Accept: application/json")
interface FeignReactorTestInterface {

  @RequestLine("GET /")
  Mono<String> justGet();

}
