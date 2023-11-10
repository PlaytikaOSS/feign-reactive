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
package reactivefeign.retry;

import feign.ExceptionPropagationPolicy;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * @author Sergii Karpenko
 */
public abstract class SimpleReactiveRetryPolicy implements ReactiveRetryPolicy {

    private final Scheduler scheduler;
    private final ExceptionPropagationPolicy exceptionPropagationPolicy;

    protected SimpleReactiveRetryPolicy(
            Scheduler scheduler,
            ExceptionPropagationPolicy exceptionPropagationPolicy) {
        this.scheduler = scheduler;
        this.exceptionPropagationPolicy = exceptionPropagationPolicy;
    }

    @Override
    public ExceptionPropagationPolicy exceptionPropagationPolicy(){
        return exceptionPropagationPolicy != null
                ? exceptionPropagationPolicy
                : ReactiveRetryPolicy.super.exceptionPropagationPolicy();
    }

    /**
   * @param error
   * @param attemptNo
   * @return -1 if should not be retried, 0 if retry immediately
   */
  abstract public long retryDelay(Throwable error, int attemptNo);

  @Override
  public Retry retry() {
    return Retry.from(errors -> errors
        .zipWith(Flux.range(1, maxAllowedRetries() + 1), (signal, index) -> {
          long delay = retryDelay(signal.failure(), index);
          if (delay >= 0) {
            return Tuples.of(delay, signal);
          } else {
            throw Exceptions.propagate(signal.failure());
          }
        }).concatMap(
            tuple2 -> tuple2.getT1() > 0
                ? Mono.delay(Duration.ofMillis(tuple2.getT1()), scheduler)
                    .map(time -> tuple2.getT2().failure())
                : Mono.just(tuple2.getT2().failure())));
  }
}
