package reactivefeign.spring.server.config;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.boot.jetty.reactive.JettyReactiveWebServerFactory;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

@Configuration
public class TestServerConfigurations {

    public static final String JETTY_H2C = "jetty-h2c";

    @Configuration
    @Profile(JETTY_H2C)
    public static class JettyConfiguration{

        @Bean
        public ReactiveWebServerFactory reactiveWebServerFactory(){
            JettyReactiveWebServerFactory jettyReactiveWebServerFactory = new JettyReactiveWebServerFactory();
            Http2 http2 = new Http2();
            http2.setEnabled(true);
            jettyReactiveWebServerFactory.setHttp2(http2);
            jettyReactiveWebServerFactory.addServerCustomizers(server -> {
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setIdleTimeout(0);
                HTTP2CServerConnectionFactory http2CFactory = new HTTP2CServerConnectionFactory(httpConfig, "h2c");
                http2CFactory.setMaxConcurrentStreams(1000);
                Arrays.stream(server.getConnectors())
                        .filter(ServerConnector.class::isInstance)
                        .map(ServerConnector.class::cast)
                        .findFirst()
                        .ifPresentOrElse(sc -> sc.addConnectionFactory(http2CFactory), () -> {
                            ServerConnector sc = new ServerConnector(server, http2CFactory);
                            server.addConnector(sc);
                        });
            });
            return jettyReactiveWebServerFactory;
        }
    }

}
