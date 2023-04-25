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
package reactivefeign.webclient.client5;

import org.apache.hc.client5.http.config.RequestConfig;
import reactivefeign.ReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class HttpClient5ReactiveOptions extends ReactiveOptions {

  private final RequestConfig requestConfig;

  private final Integer socketTimeoutMillis;
  private final Integer connectionsDefaultMaxPerRoute;
  private final Integer connectionsMaxTotal;

  private HttpClient5ReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis,
                                     Boolean tryUseCompression, Boolean followRedirects,
                                     ProxySettings proxySettings,
                                     RequestConfig requestConfig,
                                     Integer socketTimeoutMillis,
                                     Integer connectionsDefaultMaxPerRoute, Integer connectionsMaxTotal) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression, followRedirects, proxySettings);

    this.requestConfig = requestConfig;
    this.socketTimeoutMillis = socketTimeoutMillis;
    this.connectionsDefaultMaxPerRoute = connectionsDefaultMaxPerRoute;
    this.connectionsMaxTotal = connectionsMaxTotal;
  }

  public RequestConfig getRequestConfig() {
    return requestConfig;
  }

  public Integer getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  public Integer getConnectionsDefaultMaxPerRoute() {
    return connectionsDefaultMaxPerRoute;
  }

  public Integer getConnectionsMaxTotal() {
    return connectionsMaxTotal;
  }

  public static class Builder extends ReactiveOptions.Builder<Builder>{
    private RequestConfig requestConfig;

    private Integer socketTimeoutMillis;

    private Integer connectionsDefaultMaxPerRoute;
    private Integer connectionsMaxTotal;

    public Builder() {}

    public Builder setRequestConfig(RequestConfig requestConfig) {
      this.requestConfig = requestConfig;
      return this;
    }

    public Builder setSocketTimeout(Integer socketTimeoutMillis) {
      this.socketTimeoutMillis = socketTimeoutMillis;
      return this;
    }

    public Builder setConnectionsDefaultMaxPerRoute(Integer connectionsDefaultMaxPerRoute) {
      this.connectionsDefaultMaxPerRoute = connectionsDefaultMaxPerRoute;
      return this;
    }

    public Builder setConnectionsMaxTotal(Integer connectionsMaxTotal) {
      this.connectionsMaxTotal = connectionsMaxTotal;
      return this;
    }

    public HttpClient5ReactiveOptions build() {
      return new HttpClient5ReactiveOptions(useHttp2, connectTimeoutMillis,
              acceptCompressed, followRedirects, proxySettings,
              requestConfig,
              socketTimeoutMillis,
              connectionsDefaultMaxPerRoute, connectionsMaxTotal);
    }
  }

}
