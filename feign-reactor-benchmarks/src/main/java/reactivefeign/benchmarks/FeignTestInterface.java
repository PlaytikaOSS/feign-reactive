package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;
import reactor.core.publisher.Mono;

@Headers("Accept: application/json")
interface FeignTestInterface {

  @RequestLine("GET /")
  String justGet();

}
