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
  private final Long responseTimeoutMillis;

  private WebReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis,
                             Long readTimeoutMillis, Long writeTimeoutMillis, Long responseTimeoutMillis,
                             Boolean tryUseCompression, Boolean followRedirects,
                             ProxySettings proxySettings) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression, followRedirects, proxySettings);

    this.readTimeoutMillis = readTimeoutMillis;
    this.writeTimeoutMillis = writeTimeoutMillis;
    this.responseTimeoutMillis = responseTimeoutMillis;
  }

  public Long getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public Long getWriteTimeoutMillis() {
    return writeTimeoutMillis;
  }

  public Long getResponseTimeoutMillis() {
    return responseTimeoutMillis;
  }

  public boolean isEmpty() {
    return super.isEmpty() && readTimeoutMillis == null && writeTimeoutMillis == null;
  }

  public static class Builder extends ReactiveOptions.Builder{
    private Long readTimeoutMillis;
    private Long writeTimeoutMillis;
    private Long responseTimeoutMillis;

    public Builder() {}

    public Builder setReadTimeoutMillis(long readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
      return this;
    }

    public Builder setWriteTimeoutMillis(long writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
      return this;
    }

    public Builder setResponseTimeoutMillis(long responseTimeoutMillis) {
      this.responseTimeoutMillis = responseTimeoutMillis;
      return this;
    }

    public WebReactiveOptions build() {
      return new WebReactiveOptions(useHttp2, connectTimeoutMillis,
              readTimeoutMillis, writeTimeoutMillis, responseTimeoutMillis,
              acceptCompressed, followRedirects, proxySettings);
    }
  }

  public static class WebProxySettings extends ProxySettings{

    private final String username;
    private final String password;
    private final Long timeout;

    protected WebProxySettings(String host, int port, String username, String password, Long timeout) {
      super(host, port);

      this.username = username;
      this.password = password;
      this.timeout = timeout;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public Long getTimeout() {
      return timeout;
    }
  }

  public static class WebProxySettingsBuilder extends ProxySettingsBuilder {

    private String username;
    private String password;
    private Long timeout;


    public ReactiveOptions.ProxySettingsBuilder username(String username) {
      this.username = username;
      return this;
    }

    public ReactiveOptions.ProxySettingsBuilder password(String password) {
      this.password = password;
      return this;
    }

    public ReactiveOptions.ProxySettingsBuilder timeout(Long timeout) {
      this.timeout = timeout;
      return this;
    }

    public WebProxySettings build() {
      return new WebProxySettings(host, port, username, password, timeout);
    }
  }
}
