package reactivefeign.wiremock;

import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServer;
import wiremock.org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import wiremock.org.eclipse.jetty.io.NetworkTrafficListener;
import wiremock.org.eclipse.jetty.server.ConnectionFactory;
import wiremock.org.eclipse.jetty.server.HttpConfiguration;
import wiremock.org.eclipse.jetty.server.HttpConnectionFactory;
import wiremock.org.eclipse.jetty.server.ServerConnector;

public class WireMockServerConfigurations {

    public static WireMockConfiguration h2cConfig(){
        return h2cConfig(false, 100);
    }

    public static WireMockConfiguration h2cConfig(boolean includeH1, int maxConcurrentCalls){
        return WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .asynchronousResponseEnabled(true)
                .httpServerFactory((options, adminRequestHandler, stubRequestHandler) ->
                        new JettyHttpServer(options, adminRequestHandler, stubRequestHandler) {
                            @Override
                            protected ServerConnector createHttpConnector(
                                    String bindAddress, int port, JettySettings jettySettings, NetworkTrafficListener listener) {

                                HttpConfiguration httpConfig = createHttpConfig(jettySettings);

                                HTTP2CServerConnectionFactory http2CFactory = new HTTP2CServerConnectionFactory(httpConfig);
                                http2CFactory.setMaxConcurrentStreams(maxConcurrentCalls);

                                return createServerConnector(
                                        bindAddress,
                                        jettySettings,
                                        port,
                                        listener,
                                        includeH1
                                                ? new ConnectionFactory[]{new HttpConnectionFactory(httpConfig), http2CFactory}
                                                : new ConnectionFactory[]{http2CFactory}
                                );
                            }
                        });
    }


}
