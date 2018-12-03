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

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.TimeUnit;

import static reactivefeign.webclient.client.WebReactiveHttpClient.webClient;

/**
 * {@link WebClient} based implementation of reactive Feign
 *
 * @author Sergii Karpenko
 */
public class WebReactiveFeign {

    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_WRITE_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

    public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> Builder<T> builder(WebClient webClient) {
        return new Builder<>(webClient);
    }

  public static class Builder<T> extends ReactiveFeign.Builder<T> {

      protected WebClient webClient;

      protected Builder() {
          this(WebClient.create());
      }

      protected Builder(WebClient webClient) {
          setWebClient(webClient);
          options(new WebReactiveOptions.Builder()
                  .setReadTimeoutMillis(DEFAULT_READ_TIMEOUT_MILLIS)
                  .setWriteTimeoutMillis(DEFAULT_WRITE_TIMEOUT_MILLIS)
                  .setConnectTimeoutMillis(DEFAULT_CONNECT_TIMEOUT_MILLIS)
                  .build());
      }

      @Override
      public Builder<T> options(ReactiveOptions options) {
          if (!options.isEmpty()) {
              WebReactiveOptions webOptions = (WebReactiveOptions)options;
              TcpClient tcpClient = TcpClient.create();
              if (options.getConnectTimeoutMillis() != null) {
                  tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                          options.getConnectTimeoutMillis().intValue());
              }
              tcpClient = tcpClient.doOnConnected(connection -> {
                                  if(webOptions.getReadTimeoutMillis() != null){
                                      connection.addHandlerLast(new ReadTimeoutHandler(
                                              webOptions.getReadTimeoutMillis(), TimeUnit.MILLISECONDS));
                                  }
                                  if(webOptions.getWriteTimeoutMillis() != null){
                                      connection.addHandlerLast(new WriteTimeoutHandler(
                                              webOptions.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS));
                                  }
                              });

              HttpClient httpClient = HttpClient.from(tcpClient);
              if (options.isTryUseCompression() != null) {
                  httpClient = httpClient.compress(true);
              }
              ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

              setWebClient(webClient.mutate().clientConnector(connector).build());
          }
          return this;
      }

      protected void setWebClient(WebClient webClient){
          this.webClient = webClient;
          clientFactory(methodMetadata -> webClient(methodMetadata, webClient));
      }
  }
}


