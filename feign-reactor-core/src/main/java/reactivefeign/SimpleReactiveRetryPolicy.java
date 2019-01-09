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
package reactivefeign;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author Sergii Karpenko
 */
public abstract class SimpleReactiveRetryPolicy implements ReactiveRetryPolicy {

  /**
   * @param error
   * @param attemptNo
   * @return -1 if should not be retried, 0 if retry immediately
   */
  abstract long retryDelay(Throwable error, int attemptNo);

  @Override
  public Function<Flux<Throwable>, Flux<Throwable>> toRetryFunction() {
    return errors -> errors
        .zipWith(Flux.range(1, Integer.MAX_VALUE), (error, index) -> {
          long delay = retryDelay(error, index);
          if (delay >= 0) {
            return Tuples.of(delay, error);
          } else {
            throw Exceptions.propagate(error);
          }
        }).flatMap(
            tuple2 -> tuple2.getT1() > 0
                ? Mono.delay(Duration.ofMillis(tuple2.getT1()))
                    .map(time -> tuple2.getT2())
                : Mono.just(tuple2.getT2()));
  }
}
