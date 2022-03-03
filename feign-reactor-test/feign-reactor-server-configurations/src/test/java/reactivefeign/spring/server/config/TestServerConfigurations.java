package reactivefeign.spring.server.config;

import io.undertow.UndertowOptions;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.Http2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static java.util.Collections.singleton;

@Configuration
public class TestServerConfigurations {

    public static final String JETTY_H2C = "jetty-h2c";
    public static final String UNDERTOW_H2C = "undertow-h2c";

    @Configuration
    @Profile(JETTY_H2C)
    public static class JettyConfiguration{

        @Bean
        public ReactiveWebServerFactory reactiveWebServerFactory(){
            JettyReactiveWebServerFactory jettyReactiveWebServerFactory = new JettyReactiveWebServerFactory();
            Http2 http2 = new Http2();
            http2.setEnabled(true);
            jettyReactiveWebServerFactory.setHttp2(http2);
            jettyReactiveWebServerFactory.setServerCustomizers(singleton(server -> {
                ServerConnector sc = (ServerConnector) server.getConnectors()[0];
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setIdleTimeout(0);
                HTTP2CServerConnectionFactory http2CFactory = new HTTP2CServerConnectionFactory(httpConfig);
                http2CFactory.setMaxConcurrentStreams(1000);
                sc.addConnectionFactory(http2CFactory);
            }));
            return jettyReactiveWebServerFactory;
        }
    }

    @Configuration
    @Profile(UNDERTOW_H2C)
    public static class UndertowConfiguration{

        @Bean
        public ReactiveWebServerFactory reactiveWebServerFactory(){
            UndertowReactiveWebServerFactory undertowReactiveWebServerFactory = new UndertowReactiveWebServerFactory();
            Http2 http2 = new Http2();
            http2.setEnabled(true);
            undertowReactiveWebServerFactory.setHttp2(http2);
            undertowReactiveWebServerFactory.addBuilderCustomizers(builder ->
                    builder.setServerOption(UndertowOptions.ENABLE_HTTP2,true));
            return undertowReactiveWebServerFactory;
        }
    }

}
