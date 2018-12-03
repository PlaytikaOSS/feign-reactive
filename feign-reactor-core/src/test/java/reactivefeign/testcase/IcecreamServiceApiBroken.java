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

import feign.Param;
import feign.RequestLine;
import reactivefeign.ReactiveContract;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API of an iceream web service with one method that doesn't returns {@link Mono} or {@link Flux}
 * and violates {@link ReactiveContract}s rules.
 *
 * @author Sergii Karpenko
 */
public interface IcecreamServiceApiBroken {

  @RequestLine("GET /icecream/orders/{orderId}")
  Mono<IceCreamOrder> findOrder(@Param("orderId") int orderId);

  /**
   * Method that doesn't respects contract.
   */
  @RequestLine("GET /icecream/orders/{orderId}")
  IceCreamOrder findOrderBlocking(@Param("orderId") int orderId);
}
