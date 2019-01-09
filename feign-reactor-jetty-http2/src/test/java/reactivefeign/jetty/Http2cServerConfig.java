package reactivefeign.jetty;

import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServer;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;

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

                                return createServerConnector(
                                        bindAddress,
                                        jettySettings,
                                        port,
                                        listener,
                                        new HTTP2CServerConnectionFactory(httpConfig)
                                );
                            }
                        });
    }

}
