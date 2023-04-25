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
import reactor.netty.http.HttpResources;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpResources;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static reactor.netty.resources.LoopResources.DEFAULT_NATIVE;

class NettyClientHttpConnectorBuilder {

    private static final Log LOG = LogFactory.getLog(NettyClientHttpConnectorBuilder.class);

    private NettyClientHttpConnectorBuilder() {
    }

    public static ClientHttpConnector buildNettyClientHttpConnector(HttpClient httpClient, WebReactiveOptions webOptions) {

        if (httpClient == null) {

            ConnectionProvider connectionProvider = webOptions.getConnectionProvider();
            if(connectionProvider == null){
                connectionProvider = TcpResources.get();
            }

            ConnectionProvider.Builder connectionProviderBuilder = connectionProvider.mutate();
            if(webOptions.getConnectionMetricsEnabled() != null){
                connectionProviderBuilder = connectionProviderBuilder.metrics(webOptions.getConnectionMetricsEnabled());
            }
            if (webOptions.getMaxConnections() != null) {
                connectionProviderBuilder = connectionProviderBuilder.maxConnections(webOptions.getMaxConnections());
            }
            if(webOptions.getConnectionMaxIdleTimeMillis() != null){
                connectionProviderBuilder = connectionProviderBuilder.maxIdleTime(
                        Duration.ofMillis(webOptions.getConnectionMaxIdleTimeMillis()));
            }
            if(webOptions.getConnectionMaxLifeTimeMillis() != null){
                connectionProviderBuilder = connectionProviderBuilder.maxLifeTime(
                        Duration.ofMillis(webOptions.getConnectionMaxLifeTimeMillis()));
            }
            if (webOptions.getPendingAcquireMaxCount() != null) {
                connectionProviderBuilder = connectionProviderBuilder.pendingAcquireMaxCount(webOptions.getPendingAcquireMaxCount());
            }
            if (webOptions.getPendingAcquireTimeoutMillis() != null) {
                connectionProviderBuilder = connectionProviderBuilder.pendingAcquireTimeout(
                        Duration.ofMillis(webOptions.getPendingAcquireTimeoutMillis()));
            }

            connectionProvider = connectionProviderBuilder.build();

            httpClient = HttpClient.create(connectionProvider)
                    .runOn(HttpResources.get(), DEFAULT_NATIVE);
        }

        if(webOptions.getMetricsEnabled() != null){
            httpClient = httpClient.metrics(webOptions.getMetricsEnabled(), Function.identity());
        }

        if (webOptions.getConnectTimeoutMillis() != null) {
            httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    webOptions.getConnectTimeoutMillis().intValue());
        }

        httpClient = httpClient.doOnConnected(connection -> {
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
            httpClient = httpClient.proxy(typeSpec -> {
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

        if (webOptions.getResponseTimeoutMillis() != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(webOptions.getResponseTimeoutMillis()));
        }

        if (webOptions.isTryUseCompression() != null) {
            httpClient = httpClient.compress(true);
        }

        if (webOptions.isFollowRedirects() != null) {
            httpClient = httpClient.followRedirect(webOptions.isFollowRedirects());
        }

        if(webOptions.getSslContext() != null){
            httpClient = httpClient.secure(sslProviderBuilder -> sslProviderBuilder.sslContext(webOptions.getSslContext()));
        }
        else if (Objects.equals(TRUE, webOptions.isDisableSslValidation())) {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                httpClient = httpClient.secure(sslProviderBuilder -> sslProviderBuilder.sslContext(sslContext));
            } catch (SSLException e) {
                LOG.warn("Error creating SSLContext. The WebClient will verify all new HTTPS calls", e);
            }
        }

        return new ReactorClientHttpConnector(httpClient);
    }
}
