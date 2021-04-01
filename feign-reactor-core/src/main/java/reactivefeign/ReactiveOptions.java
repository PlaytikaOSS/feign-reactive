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
abstract public class ReactiveOptions {

  private final Boolean useHttp2;
  private final Long connectTimeoutMillis;
  private final Boolean acceptCompressed;
  private final Boolean followRedirects;
  private final ProxySettings proxySettings;

  protected ReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis, Boolean acceptCompressed,
                            Boolean followRedirects, ProxySettings proxySettings) {
    this.useHttp2 = useHttp2;
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.acceptCompressed = acceptCompressed;
    this.followRedirects = followRedirects;
    this.proxySettings = proxySettings;
  }

  public Boolean getUseHttp2() {
    return useHttp2;
  }

  public Long getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public Boolean isTryUseCompression() {
    return acceptCompressed;
  }

  public Boolean isFollowRedirects() {
    return followRedirects;
  }

  public ProxySettings getProxySettings() {
    return proxySettings;
  }

  public boolean isEmpty() {
    return useHttp2 == null
            && connectTimeoutMillis == null
            && acceptCompressed == null
            && followRedirects == null
            && proxySettings == null;
  }

  public static boolean useHttp2(ReactiveOptions options){
    return options != null && options.getUseHttp2() != null && options.getUseHttp2();
  }


  abstract public static class Builder {
    protected Boolean useHttp2;
    protected Long connectTimeoutMillis;
    protected Boolean acceptCompressed;
    protected Boolean followRedirects;
    protected ProxySettings proxySettings;

    public Builder() {}

    public Builder setUseHttp2(boolean useHttp2) {
      this.useHttp2 = useHttp2;
      return this;
    }

    public Builder setConnectTimeoutMillis(long connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
      return this;
    }

    public Builder setAcceptCompressed(boolean acceptCompressed) {
      this.acceptCompressed = acceptCompressed;
      return this;
    }

    public Builder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public Builder setProxySettings(ProxySettings proxySettings) {
      this.proxySettings = proxySettings;
      return this;
    }

    abstract public ReactiveOptions build();
  }

  public static class ProxySettings {
    private final String host;
    private final int port;

    protected ProxySettings(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

  }
  
  public static class ProxySettingsBuilder {
    protected String host;
    protected int port;

    public ProxySettingsBuilder host(String host) {
      this.host = host;
      return this;
    }

    public ProxySettingsBuilder port(int port) {
      this.port = port;
      return this;
    }

    public ProxySettings build(){
      return new ProxySettings(host, port);
    }

  }
}
