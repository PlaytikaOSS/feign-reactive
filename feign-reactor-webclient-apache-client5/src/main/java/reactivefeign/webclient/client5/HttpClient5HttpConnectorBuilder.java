package reactivefeign.webclient.client5;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.H2AsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import reactivefeign.ReactiveOptions;

import java.util.concurrent.TimeUnit;

class HttpClient5HttpConnectorBuilder {

    public static ClientHttpConnector buildHttp5ClientHttpConnector(HttpClient5ReactiveOptions options) {

        RequestConfig requestConfig = buildRequestConfig(options);

        CloseableHttpAsyncClient client;
        if(ReactiveOptions.useHttp2(options)){
            H2AsyncClientBuilder clientBuilder = HttpAsyncClients.customHttp2()
                    .setDefaultRequestConfig(requestConfig)
                    .disableAutomaticRetries();

            ReactiveOptions.ProxySettings proxySettings = options.getProxySettings();
            if (proxySettings != null) {
                clientBuilder = clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(
                        new HttpHost(proxySettings.getHost(), proxySettings.getPort())));
            }

            client = clientBuilder.build();
        } else {
            HttpAsyncClientBuilder clientBuilder = HttpAsyncClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .disableAutomaticRetries();

            clientBuilder = clientBuilder.setConnectionManager(buildConnectionManager(options));

            if(options != null) {
                ReactiveOptions.ProxySettings proxySettings = options.getProxySettings();
                if (proxySettings != null) {
                    clientBuilder = clientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(
                            new HttpHost(proxySettings.getHost(), proxySettings.getPort())));
                }
            }

            client = clientBuilder.build();
        }

        return new HttpComponentsClientHttpConnector(client);
    }

    private static PoolingAsyncClientConnectionManager buildConnectionManager(HttpClient5ReactiveOptions options) {

        PoolingAsyncClientConnectionManager connectionManager = new PoolingAsyncClientConnectionManager();
        if(options != null){
            if(options.getConnectionsDefaultMaxPerRoute() != null){
                connectionManager.setDefaultMaxPerRoute(options.getConnectionsDefaultMaxPerRoute());
            }
            if(options.getConnectionsMaxTotal() != null){
                connectionManager.setMaxTotal(options.getConnectionsMaxTotal());
            }
        }

        return connectionManager;
    }

    private static RequestConfig buildRequestConfig(HttpClient5ReactiveOptions options) {
        RequestConfig.Builder configBuilder = options != null && options.getRequestConfig() != null
                ? RequestConfig.copy(options.getRequestConfig()) : RequestConfig.custom();

        if(options != null) {
            if (options.getConnectTimeoutMillis() != null) {
                configBuilder = configBuilder.setConnectTimeout(options.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            }
            if(options.getSocketTimeoutMillis() != null){
                configBuilder = configBuilder.setResponseTimeout(options.getSocketTimeoutMillis(), TimeUnit.MILLISECONDS);
            }


            if (options.isFollowRedirects() != null) {
                configBuilder = configBuilder.setRedirectsEnabled(options.isFollowRedirects());
            }

//            if (options.getProxySettings() != null) {
//                ReactiveOptions.ProxySettings proxySettings = options.getProxySettings();
//                configBuilder = configBuilder.setProxy(new HttpHost(proxySettings.getHost(), proxySettings.getPort()));
//            }

            if (options.isTryUseCompression() != null) {
                configBuilder = configBuilder.setContentCompressionEnabled(options.isTryUseCompression());
            }
        }

        return configBuilder.build();
    }
}
