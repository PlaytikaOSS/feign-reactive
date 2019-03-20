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
package reactivefeign.webclient;

import reactivefeign.ReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class WebReactiveOptions extends ReactiveOptions {

  public static final WebReactiveOptions DEFAULT_OPTIONS = (WebReactiveOptions)new WebReactiveOptions.Builder()
          .setReadTimeoutMillis(10000)
          .setWriteTimeoutMillis(10000)
          .setConnectTimeoutMillis(5000)
          .build();

  private final Long readTimeoutMillis;
  private final Long writeTimeoutMillis;

  private WebReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis, Long readTimeoutMillis,
                             Long writeTimeoutMillis, Boolean tryUseCompression) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression);

    this.readTimeoutMillis = readTimeoutMillis;
    this.writeTimeoutMillis = writeTimeoutMillis;
  }

  public Long getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public Long getWriteTimeoutMillis() {
    return writeTimeoutMillis;
  }

  public boolean isEmpty() {
    return super.isEmpty() && readTimeoutMillis == null && writeTimeoutMillis == null;
  }

  public static class Builder extends ReactiveOptions.Builder{
    private Long readTimeoutMillis;
    private Long writeTimeoutMillis;

    public Builder() {}

    public Builder setReadTimeoutMillis(long readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
      return this;
    }

    public Builder setWriteTimeoutMillis(long writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
      return this;
    }

    public WebReactiveOptions build() {
      return new WebReactiveOptions(useHttp2, connectTimeoutMillis, readTimeoutMillis,
              writeTimeoutMillis, acceptCompressed);
    }
  }
}
