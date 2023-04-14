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
package reactivefeign.java11;

import reactivefeign.ReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class Java11ReactiveOptions extends ReactiveOptions {

  private final Long requestTimeoutMillis;

  private Java11ReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis, Long requestTimeoutMillis,
                                Boolean tryUseCompression, Boolean followRedirects, ProxySettings proxySettings) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression, followRedirects, proxySettings);

    this.requestTimeoutMillis = requestTimeoutMillis;
  }

  public Long getRequestTimeoutMillis() {
    return requestTimeoutMillis;
  }

  public static class Builder extends ReactiveOptions.Builder<Builder>{
    private Long requestTimeoutMillis;

    public Builder() {}

    public Builder setRequestTimeoutMillis(long requestTimeoutMillis) {
      this.requestTimeoutMillis = requestTimeoutMillis;
      return this;
    }

    public Java11ReactiveOptions build() {
      return new Java11ReactiveOptions(useHttp2, connectTimeoutMillis, requestTimeoutMillis,
              acceptCompressed, followRedirects, proxySettings);
    }
  }
}
