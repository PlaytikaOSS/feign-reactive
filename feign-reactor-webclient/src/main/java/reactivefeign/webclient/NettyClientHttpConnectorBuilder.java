package reactivefeign.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class NettyClientHttpConnectorBuilder {

    public static ClientHttpConnector buildNettyClientHttpConnector(WebReactiveOptions webOptions) {
        TcpClient tcpClient = TcpClient.create();
        if (webOptions.getConnectTimeoutMillis() != null) {
            tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    webOptions.getConnectTimeoutMillis().intValue());
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

        WebReactiveOptions.WebProxySettings proxySettings = (WebReactiveOptions.WebProxySettings)webOptions.getProxySettings();
        if(proxySettings != null){
            tcpClient = tcpClient.proxy(typeSpec -> {
                ProxyProvider.Builder proxyBuilder = typeSpec.type(ProxyProvider.Proxy.HTTP)
                        .host(proxySettings.getHost())
                        .port(proxySettings.getPort())
                        .username(proxySettings.getUsername())
                        .password(password -> proxySettings.getPassword());
                if(proxySettings.getTimeout() != null){
                    proxyBuilder.connectTimeoutMillis(proxySettings.getTimeout());
                }
            });
        }

        HttpClient httpClient = HttpClient.from(tcpClient);

        if(webOptions.getResponseTimeoutMillis() != null){
            httpClient = httpClient.responseTimeout(Duration.ofMillis(webOptions.getResponseTimeoutMillis()));
        }

        if (webOptions.isTryUseCompression() != null) {
            httpClient = httpClient.compress(true);
        }
        if(webOptions.isFollowRedirects() != null){
            httpClient = httpClient.followRedirect(webOptions.isFollowRedirects());
        }
        return new ReactorClientHttpConnector(httpClient);
    }
}
