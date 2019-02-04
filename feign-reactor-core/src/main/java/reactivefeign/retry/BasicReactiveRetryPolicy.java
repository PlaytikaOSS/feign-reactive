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

import feign.RetryableException;

import java.util.Date;

/**
 * @author Sergii Karpenko
 */
public class BasicReactiveRetryPolicy extends SimpleReactiveRetryPolicy{

  private final int maxRetries;
  private final long periodInMs;

  private BasicReactiveRetryPolicy(int maxRetries, long periodInMs){
    this.maxRetries = maxRetries;
    this.periodInMs = periodInMs;
  }

  public static SimpleReactiveRetryPolicy retry(int maxRetries) {
    return new BasicReactiveRetryPolicy(maxRetries, 0);
  }

  public static SimpleReactiveRetryPolicy retryWithBackoff(int maxRetries, long periodInMs) {
    return new BasicReactiveRetryPolicy(maxRetries, periodInMs);
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
          delay = retryAfter.getTime() - System.currentTimeMillis();
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

  public static class Builder implements ReactiveRetryPolicy.Builder{
    private int maxRetries;
    private long backoffInMs = 0;

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public long getBackoffInMs() {
      return backoffInMs;
    }

    public void setBackoffInMs(long backoffInMs) {
      this.backoffInMs = backoffInMs;
    }

    public BasicReactiveRetryPolicy build(){
      return new BasicReactiveRetryPolicy(maxRetries, backoffInMs);
    }
  }
}
