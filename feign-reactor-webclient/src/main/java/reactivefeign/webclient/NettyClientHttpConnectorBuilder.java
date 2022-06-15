package reactivefeign.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class NettyClientHttpConnectorBuilder {

    private static final Log LOG = LogFactory.getLog(NettyClientHttpConnectorBuilder.class);

    public static ClientHttpConnector buildNettyClientHttpConnector(WebReactiveOptions webOptions) {
        TcpClient tcpClient = TcpClient.create();
        if (webOptions.getConnectTimeoutMillis() != null) {
            tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    webOptions.getConnectTimeoutMillis().intValue());
        }
        tcpClient = tcpClient.doOnConnected(connection -> {
            if (webOptions.getReadTimeoutMillis() != null) {
                connection.addHandlerLast(new ReadTimeoutHandler(
                        webOptions.getReadTimeoutMillis(), TimeUnit.MILLISECONDS));
            }
            if (webOptions.getWriteTimeoutMillis() != null) {
                connection.addHandlerLast(new WriteTimeoutHandler(
                        webOptions.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS));
            }
        });

        WebReactiveOptions.WebProxySettings proxySettings = (WebReactiveOptions.WebProxySettings)webOptions.getProxySettings();
        if (proxySettings != null) {
            tcpClient = tcpClient.proxy(typeSpec -> {
                ProxyProvider.Builder proxyBuilder = typeSpec.type(ProxyProvider.Proxy.HTTP)
                        .host(proxySettings.getHost())
                        .port(proxySettings.getPort())
                        .username(proxySettings.getUsername())
                        .password(password -> proxySettings.getPassword());
                if (proxySettings.getTimeout() != null) {
                    proxyBuilder.connectTimeoutMillis(proxySettings.getTimeout());
                }
            });
        }

        HttpClient httpClient = HttpClient.from(tcpClient);

        if (webOptions.getResponseTimeoutMillis() != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(webOptions.getResponseTimeoutMillis()));
        }

        if (webOptions.isTryUseCompression() != null) {
            httpClient = httpClient.compress(true);
        }

        if (webOptions.isFollowRedirects() != null) {
            httpClient = httpClient.followRedirect(webOptions.isFollowRedirects());
        }

        if (Objects.equals(Boolean.TRUE, webOptions.isDisableSslValidation())) {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                httpClient = httpClient.secure(t -> t.sslContext(sslContext));
            } catch (SSLException e) {
                LOG.warn("Error creating SSLContext. The WebClient will verify all new HTTPS calls", e);
            }
        }

        return new ReactorClientHttpConnector(httpClient);
    }
}
