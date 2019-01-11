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
package reactivefeign.rx2.client.statushandler;

import io.reactivex.Single;
import reactivefeign.client.ReactiveHttpResponse;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class Rx2StatusHandlers {

  private Rx2StatusHandlers(){}

  public static Rx2StatusHandler throwOnStatus(
          Predicate<Integer> statusPredicate,
          BiFunction<String, ReactiveHttpResponse, Throwable> errorFunction) {
    return new Rx2StatusHandler() {
      @Override
      public boolean shouldHandle(int status) {
        return statusPredicate.test(status);
      }

      @Override
      public Single<? extends Throwable> decode(String methodKey, ReactiveHttpResponse response) {
        return Single.just(errorFunction.apply(methodKey, response));
      }
    };
  }
}
