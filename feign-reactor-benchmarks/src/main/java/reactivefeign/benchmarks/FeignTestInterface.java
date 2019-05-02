package reactivefeign.benchmarks;

import feign.Headers;
import feign.RequestLine;

import java.util.Map;

import static reactivefeign.benchmarks.RealRequestBenchmarks.PATH_WITH_PAYLOAD;

@Headers("Accept: application/json")
interface FeignTestInterface {

  @RequestLine("GET /")
  String justGet();

  @RequestLine("POST "+PATH_WITH_PAYLOAD)
  Map<String, Object> postWithPayload(Map<String, Object> payload);
}
