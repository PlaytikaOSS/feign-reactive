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

/**
 * @author Sergii Karpenko
 */
public class ReactiveOptions {

  private final Long connectTimeoutMillis;
  private final Boolean tryUseCompression;

  protected ReactiveOptions(Long connectTimeoutMillis, Boolean tryUseCompression) {
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.tryUseCompression = tryUseCompression;
  }

  public Long getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public Boolean isTryUseCompression() {
    return tryUseCompression;
  }

  public boolean isEmpty() {
    return connectTimeoutMillis == null && tryUseCompression == null;
  }

  abstract public static class Builder {
    protected Long connectTimeoutMillis;
    protected Boolean tryUseCompression;

    public Builder() {}

    public Builder setConnectTimeoutMillis(long connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
      return this;
    }

    public Builder setTryUseCompression(boolean tryUseCompression) {
      this.tryUseCompression = tryUseCompression;
      return this;
    }

    abstract public ReactiveOptions build();
  }
}
