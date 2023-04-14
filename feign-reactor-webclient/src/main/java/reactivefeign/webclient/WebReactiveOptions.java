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

import io.netty.handler.ssl.SslContext;
import reactivefeign.ReactiveOptions;
import reactor.netty.resources.ConnectionProvider;

/**
 * @author Sergii Karpenko
 * @author Sebastian Nawrocki
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
  private final SslContext sslContext;
  private final Boolean disableSslValidation;
  private final Boolean metricsEnabled;

  private final ConnectionProvider connectionProvider;
  private final Integer maxConnections;
  private final Boolean connectionMetricsEnabled;
  private final Long connectionMaxIdleTimeMillis;
  private final Long connectionMaxLifeTimeMillis;
  private final Integer pendingAcquireMaxCount;
  private final Long pendingAcquireTimeoutMillis;

  private WebReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis,
                             Long readTimeoutMillis, Long writeTimeoutMillis, Long responseTimeoutMillis,
                             Boolean tryUseCompression, Boolean followRedirects,
                             ProxySettings proxySettings,
                             SslContext sslContext, Boolean disableSslValidation,
                             Boolean metricsEnabled,
                             ConnectionProvider connectionProvider,
                             Integer maxConnections, Boolean connectionMetricsEnabled,
                             Long connectionMaxIdleTimeMillis, Long connectionMaxLifeTimeMillis,
                             Integer pendingAcquireMaxCount, Long pendingAcquireTimeoutMillis) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression, followRedirects, proxySettings);

    this.readTimeoutMillis = readTimeoutMillis;
    this.writeTimeoutMillis = writeTimeoutMillis;
    this.responseTimeoutMillis = responseTimeoutMillis;
    this.sslContext = sslContext;
    this.disableSslValidation = disableSslValidation;
    this.metricsEnabled = metricsEnabled;
    this.connectionProvider = connectionProvider;
    this.maxConnections = maxConnections;
    this.connectionMetricsEnabled = connectionMetricsEnabled;
    this.connectionMaxIdleTimeMillis = connectionMaxIdleTimeMillis;
    this.connectionMaxLifeTimeMillis = connectionMaxLifeTimeMillis;
    this.pendingAcquireMaxCount = pendingAcquireMaxCount;
    this.pendingAcquireTimeoutMillis = pendingAcquireTimeoutMillis;
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

  public Boolean isDisableSslValidation() {
    return disableSslValidation;
  }

  public SslContext getSslContext() {
    return sslContext;
  }

  public Boolean getMetricsEnabled() {
    return metricsEnabled;
  }

  public ConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public Integer getMaxConnections() {
    return maxConnections;
  }

  public Boolean getConnectionMetricsEnabled() {
    return connectionMetricsEnabled;
  }

  public Long getConnectionMaxIdleTimeMillis() {
    return connectionMaxIdleTimeMillis;
  }

  public Long getConnectionMaxLifeTimeMillis() {
    return connectionMaxLifeTimeMillis;
  }

  public Integer getPendingAcquireMaxCount() {
    return pendingAcquireMaxCount;
  }

  public Long getPendingAcquireTimeoutMillis() {
    return pendingAcquireTimeoutMillis;
  }

  public static class Builder extends ReactiveOptions.Builder<Builder> {
    private Long readTimeoutMillis;
    private Long writeTimeoutMillis;
    private Long responseTimeoutMillis;
    private Boolean disableSslValidation;
    private SslContext sslContext;
    private Boolean metricsEnabled;
    private ConnectionProvider connectionProvider;
    private Integer maxConnections;
    private Boolean connectionMetricsEnabled;
    private Long connectionMaxIdleTimeMillis;
    private Long connectionMaxLifeTimeMillis;
    private Integer pendingAcquireMaxCount;
    private Long pendingAcquireTimeoutMillis;

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

    public Builder setDisableSslValidation(boolean disableSslValidation) {
      this.disableSslValidation = disableSslValidation;
      return this;
    }

    public Builder setSslContext(SslContext sslContext) {
      this.sslContext = sslContext;
      return this;
    }

    public Builder setMetricsEnabled(Boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
      return this;
    }

    public Builder setConnectionProvider(ConnectionProvider connectionProvider) {
      this.connectionProvider = connectionProvider;
      return this;
    }

    public Builder setConnectionMetricsEnabled(Boolean connectionMetricsEnabled) {
      this.connectionMetricsEnabled = connectionMetricsEnabled;
      return this;
    }

    public Builder setMaxConnections(Integer maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    public Builder setConnectionMaxIdleTimeMillis(Long connectionMaxIdleTimeMillis) {
      this.connectionMaxIdleTimeMillis = connectionMaxIdleTimeMillis;
      return this;
    }

    public Builder setConnectionMaxLifeTimeMillis(Long connectionMaxLifeTimeMillis) {
      this.connectionMaxLifeTimeMillis = connectionMaxLifeTimeMillis;
      return this;
    }

    public Builder setPendingAcquireMaxCount(Integer pendingAcquireMaxCount) {
      this.pendingAcquireMaxCount = pendingAcquireMaxCount;
      return this;
    }

    public Builder setPendingAcquireTimeoutMillis(Long pendingAcquireTimeoutMillis) {
      this.pendingAcquireTimeoutMillis = pendingAcquireTimeoutMillis;
      return this;
    }

    public WebReactiveOptions build() {
      return new WebReactiveOptions(useHttp2, connectTimeoutMillis,
              readTimeoutMillis, writeTimeoutMillis, responseTimeoutMillis,
              acceptCompressed, followRedirects, proxySettings,
              sslContext, disableSslValidation,
              metricsEnabled,
              connectionProvider,
              maxConnections, connectionMetricsEnabled,
              connectionMaxIdleTimeMillis, connectionMaxLifeTimeMillis,
              pendingAcquireMaxCount, pendingAcquireTimeoutMillis);
    }
  }

  public static class WebProxySettings extends ProxySettings {

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


    public WebProxySettingsBuilder username(String username) {
      this.username = username;
      return this;
    }

    public WebProxySettingsBuilder password(String password) {
      this.password = password;
      return this;
    }

    public WebProxySettingsBuilder timeout(Long timeout) {
      this.timeout = timeout;
      return this;
    }

    public WebProxySettings build() {
      return new WebProxySettings(host, port, username, password, timeout);
    }
  }
}
