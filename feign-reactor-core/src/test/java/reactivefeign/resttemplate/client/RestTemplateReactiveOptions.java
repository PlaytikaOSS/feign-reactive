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
package reactivefeign.resttemplate.client;

import reactivefeign.ReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class RestTemplateReactiveOptions extends ReactiveOptions {

  private final Long readTimeoutMillis;

  private RestTemplateReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis, Long readTimeoutMillis, Boolean tryUseCompression) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression);

    this.readTimeoutMillis = readTimeoutMillis;
  }

  public Long getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public boolean isEmpty() {
    return super.isEmpty() && readTimeoutMillis == null;
  }

  public static class Builder extends ReactiveOptions.Builder{
    private Long readTimeoutMillis;

    public Builder() {}

    public Builder setReadTimeoutMillis(long readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
      return this;
    }

    public RestTemplateReactiveOptions build() {
      return new RestTemplateReactiveOptions(useHttp2, connectTimeoutMillis, readTimeoutMillis, acceptCompressed);
    }
  }
}
