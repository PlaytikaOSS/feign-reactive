package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;
import reactor.core.publisher.Mono;

import java.util.Map;

@Headers("Accept: application/json")
interface FeignTestInterface {

  @RequestLine("GET /")
  String justGet();

  @RequestLine("POST /postWithPayload")
  Map<String, Object> postWithPayload(Map<String, Object> payload);
}
