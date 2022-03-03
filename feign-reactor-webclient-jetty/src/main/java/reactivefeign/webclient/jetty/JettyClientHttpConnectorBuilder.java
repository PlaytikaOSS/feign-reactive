package reactivefeign.webclient.jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import reactivefeign.ReactiveOptions;

class JettyClientHttpConnectorBuilder {

    public static ClientHttpConnector buildJettyClientHttpConnector(JettyReactiveOptions options) {

        boolean useHttp2 = ReactiveOptions.useHttp2(options);
        HttpClient httpClient;
        if(useHttp2){
            HTTP2Client h2Client = new HTTP2Client();
            h2Client.setSelectors(1);
            HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

            httpClient = new HttpClient(transport);
        } else {
             httpClient = new HttpClient();
        }
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (options.getConnectTimeoutMillis() != null) {
            httpClient.setConnectTimeout(options.getConnectTimeoutMillis());
        }

        if(options.isFollowRedirects() != null){
            httpClient.setFollowRedirects(options.isFollowRedirects());
        }

        if(options.getProxySettings() != null){
            ReactiveOptions.ProxySettings proxySettings = options.getProxySettings();
            httpClient.getProxyConfiguration().getProxies()
                    .add(new HttpProxy(proxySettings.getHost(), proxySettings.getPort()));
        }

        return new JettyClientHttpConnector(httpClient);
    }
}
