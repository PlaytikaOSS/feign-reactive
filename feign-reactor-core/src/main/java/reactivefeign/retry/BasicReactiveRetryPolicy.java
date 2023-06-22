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
import feign.RetryableException;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.util.Date;

/**
 * @author Sergii Karpenko
 */
public class BasicReactiveRetryPolicy extends SimpleReactiveRetryPolicy{

  private final int maxRetries;
  private final long periodInMs;
  private final Clock clock;

  private BasicReactiveRetryPolicy(
          int maxRetries, long periodInMs,
          Clock clock, Scheduler scheduler,
          ExceptionPropagationPolicy exceptionPropagationPolicy){
    super(scheduler, exceptionPropagationPolicy);
    this.maxRetries = maxRetries;
    this.periodInMs = periodInMs;
    this.clock = clock;
  }

  public static SimpleReactiveRetryPolicy retry(int maxRetries) {
    return new BasicReactiveRetryPolicy.Builder().setMaxRetries(maxRetries).build();
  }

  public static SimpleReactiveRetryPolicy retryWithBackoff(int maxRetries, long periodInMs) {
    return new BasicReactiveRetryPolicy.Builder().setMaxRetries(maxRetries).setBackoffInMs(periodInMs).build();
  }

  public static SimpleReactiveRetryPolicy retryWithBackoff(int maxRetries, long periodInMs, Scheduler scheduler) {
    return new Builder().setMaxRetries(maxRetries).setBackoffInMs(periodInMs).setScheduler(scheduler).build();
  }

  @Override
  public long retryDelay(Throwable error, int attemptNo) {
    if (attemptNo <= maxRetries) {
      if(periodInMs > 0) {
        long delay;
        Date retryAfter;
        // "Retry-After" header set
        if (error instanceof RetryableException
                && (retryAfter = ((RetryableException) error)
                .retryAfter()) != null) {
          delay = retryAfter.getTime() - clock.millis();
          delay = Math.min(delay, periodInMs);
          delay = Math.max(delay, 0);
        } else {
          delay = periodInMs;
        }
        return delay;
      } else {
        return 0;
      }
    } else {
      return -1;
    }
  }

  @Override
  public int maxAllowedRetries() {
    return maxRetries;
  }

  public static class Builder implements ReactiveRetryPolicy.Builder{
    private int maxRetries;
    private long backoffInMs = 0;
    private ExceptionPropagationPolicy exceptionPropagationPolicy;
    private Scheduler scheduler = Schedulers.parallel();
    private Clock clock = Clock.systemUTC();

    public Builder setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder setBackoffInMs(long backoffInMs) {
      this.backoffInMs = backoffInMs;
      return this;
    }

    public Builder setExceptionPropagationPolicy(ExceptionPropagationPolicy exceptionPropagationPolicy) {
      this.exceptionPropagationPolicy = exceptionPropagationPolicy;
      return this;
    }

    Builder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    Builder setScheduler(Scheduler scheduler) {
      this.scheduler = scheduler;
      return this;
    }

    public BasicReactiveRetryPolicy build(){
      return new BasicReactiveRetryPolicy(maxRetries, backoffInMs, clock, scheduler, exceptionPropagationPolicy);
    }
  }
}
