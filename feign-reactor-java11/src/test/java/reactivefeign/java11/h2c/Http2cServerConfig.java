package reactivefeign.java11.h2c;

import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServer;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;

import static reactivefeign.ReactivityTest.CALLS_NUMBER;

public class Http2cServerConfig {

    public static WireMockConfiguration wireMockConfig(){
        return WireMockConfiguration.wireMockConfig()
                .httpServerFactory((options, adminRequestHandler, stubRequestHandler) ->
                        new JettyHttpServer(options, adminRequestHandler, stubRequestHandler) {
                            @Override
                            protected ServerConnector createHttpConnector(
                                    String bindAddress,
                                    int port,
                                    JettySettings jettySettings,
                                    NetworkTrafficListener listener) {

                                HttpConfiguration httpConfig = createHttpConfig(jettySettings);
                                HTTP2CServerConnectionFactory http2CFactory = new HTTP2CServerConnectionFactory(httpConfig);
                                http2CFactory.setMaxConcurrentStreams(CALLS_NUMBER);

                                return createServerConnector(
                                        bindAddress,
                                        jettySettings,
                                        port,
                                        listener,
                                        new HttpConnectionFactory(httpConfig),
                                        http2CFactory
                                );
                            }
                        });
    }

}
