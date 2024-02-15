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
package reactivefeign.rx3.testcase;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import reactivefeign.rx3.testcase.domain.Bill;
import reactivefeign.rx3.testcase.domain.Flavor;
import reactivefeign.rx3.testcase.domain.IceCreamOrder;
import reactivefeign.rx3.testcase.domain.Mixin;

/**
 * API of an iceream web service.
 *
 * @author Sergii Karpenko
 */
@Headers({"Accept: application/json"})
public interface IcecreamServiceApi {

  RuntimeException RUNTIME_EXCEPTION = new RuntimeException("tests exception");

  @RequestLine("GET /icecream/flavors")
  Flowable<Flavor> getAvailableFlavors();

  @RequestLine("GET /icecream/mixins")
  Observable<Mixin> getAvailableMixins();

  @RequestLine("POST /icecream/orders")
  @Headers("Content-Type: application/json")
  Single<Bill> makeOrder(IceCreamOrder order);

  @RequestLine("GET /icecream/orders/{orderId}")
  Maybe<IceCreamOrder> findOrder(@Param("orderId") int orderId);

  @RequestLine("POST /icecream/bills/pay")
  @Headers("Content-Type: application/json")
  Single<Long> payBill(Bill bill);

  default Maybe<IceCreamOrder> findFirstOrder() {
    return findOrder(1);
  }

  default Single<IceCreamOrder> throwsException() {
    throw RUNTIME_EXCEPTION;
  }
}
