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

  public static boolean useHttp2(ReactiveOptions options){
    return options != null && options.getUseHttp2() != null && options.getUseHttp2();
  }


  abstract public static class Builder <B extends Builder<B>> {
    protected Boolean useHttp2;
    protected Long connectTimeoutMillis;
    protected Boolean acceptCompressed;
    protected Boolean followRedirects;
    protected ProxySettings proxySettings;

    public Builder() {}

    public B setUseHttp2(boolean useHttp2) {
      this.useHttp2 = useHttp2;
      return (B)this;
    }

    public B setConnectTimeoutMillis(long connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
      return (B)this;
    }

    public B setAcceptCompressed(boolean acceptCompressed) {
      this.acceptCompressed = acceptCompressed;
      return (B)this;
    }

    public B setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return (B)this;
    }

    public B setProxySettings(ProxySettings proxySettings) {
      this.proxySettings = proxySettings;
      return (B)this;
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
