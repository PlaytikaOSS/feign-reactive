/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.testcase;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.Flavor;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.Mixin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * API of an iceream web service.
 *
 * @author Sergii Karpenko
 */
@Headers({"Accept: application/json"})
public interface IcecreamServiceApi {

  RuntimeException RUNTIME_EXCEPTION = new RuntimeException("tests exception");
  String UPPER_HEADER_TO_REMOVE = "Header-To-Remove";

  @RequestLine("GET /icecream/flavors")
  Flux<Flavor> getAvailableFlavors();

  @RequestLine("GET /icecream/mixins")
  Flux<Mixin> getAvailableMixins();

  @RequestLine("POST /icecream/orders")
  @Headers("Content-Type: application/json")
  Mono<Bill> makeOrder(IceCreamOrder order);

  @RequestLine("POST /icecream/orders/batch")
  @Headers("Content-Type: application/json")
  Flux<Bill> makeOrders(Flux<IceCreamOrder> orders);

  @RequestLine("GET /icecream/orders/{orderId}")
  @Headers(UPPER_HEADER_TO_REMOVE + ": something-to-remove")
  Mono<IceCreamOrder> findOrder(@Param("orderId") int orderId);

  @RequestLine("GET /icecream/orders/redirect/{orderId}")
  Mono<IceCreamOrder> findOrderWithRedirect(@Param("orderId") int orderId);

  @RequestLine("POST /icecream/bills/pay")
  @Headers("Content-Type: application/json")
  Mono<Void> payBill(Bill bill);

  @RequestLine("GET /ping")
  @Headers("Content-Type: application/json")
  Mono<Void> ping();

  @RequestLine("POST /genericJson")
  Mono<Map<String, Object>> genericJson(Map<String, Object> payload);

  @RequestLine("GET /icecream/flavors")
  Mono<ReactiveHttpResponse<Flux<Flavor>>> response();

  @RequestLine("GET /mirrorParameters/{parameterInPathPlaceholder}?paramInUrl={paramInQueryPlaceholder}")
  Mono<Void> passParameters(
          @Param("parameterInPathPlaceholder") String paramInPath,
          @Param("paramInQueryPlaceholder") String paramInQuery,
          @QueryMap Map<String, String> paramMap);

  default Mono<IceCreamOrder> findFirstOrder() {
    return findOrder(1);
  }

  default Flux<String> getAvailableMixinNames() {
    return getAvailableMixins().map(Enum::name);
  }

  default Mono<IceCreamOrder> throwsException() {
    throw RUNTIME_EXCEPTION;
  }

}
