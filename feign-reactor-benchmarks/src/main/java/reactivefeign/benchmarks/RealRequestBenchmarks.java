package reactivefeign.benchmarks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Util;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.java11.Java11ReactiveFeign;
import reactivefeign.java11.Java11ReactiveOptions;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;
import reactivefeign.webclient.WebReactiveFeign;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static reactivefeign.benchmarks.BenchmarkUtils.readJsonFromFile;
import static reactivefeign.benchmarks.BenchmarkUtils.readJsonFromFileAsBytes;

abstract public class RealRequestBenchmarks {

    public static final String PATH_WITH_PAYLOAD = "/withPayload";
    public static final int SERVER_PORT = 8766;
    public static final int SERVER_H2C_PORT = 8767;
    public static final String SERVER_URL = "http://localhost:" + SERVER_H2C_PORT;
    public static final String SERVER_H2C_URL = "http://localhost:" + SERVER_H2C_PORT;

//    private HttpServer<ByteBuf, ByteBuf> serverRx;
    private Server serverJettyH2c;
//    private WireMockServer serverWireMockH2c;

    protected WebClient webClient;

    protected FeignReactorTestInterface webClientFeign;

    protected HttpClient jettyHttpClient;
    protected FeignReactorTestInterface jettyFeign;
    protected HttpClient jettyH2cClient;
    protected FeignReactorTestInterface jettyFeignH2c;

    protected FeignReactorTestInterface java11Feign;

    protected FeignReactorTestInterface java11FeignH2c;

    protected FeignTestInterface feign;

    protected byte[] responseJson;
    protected Map<String, Object> requestPayload;
    protected ObjectMapper objectMapper;

    protected void setup() throws Exception {

        responseJson = readJsonFromFileAsBytes("/response.json");

        objectMapper = new ObjectMapper();
        requestPayload = objectMapper.readValue(readJsonFromFile("/request.json"), HashMap.class);

//        serverRx = rxNetty(SERVER_PORT);
//        serverRx.start();

        //H2C
        serverJettyH2c = jettyH2c(SERVER_H2C_PORT);
        serverJettyH2c.start();
//        serverWireMockH2c = wireMockH2c(SERVER_H2C_PORT);
//        serverWireMockH2c.start();

        webClient = WebClient.create();

        webClientFeign = WebReactiveFeign.<FeignReactorTestInterface>builder()
                .target(FeignReactorTestInterface.class, SERVER_URL);

        jettyHttpClient = new HttpClient();
        jettyHttpClient.setMaxConnectionsPerDestination(10000);
        jettyHttpClient.start();
        jettyFeign = JettyReactiveFeign.<FeignReactorTestInterface>builder(jettyHttpClient)
                .target(FeignReactorTestInterface.class, SERVER_URL);

        HTTP2Client h2Client = new HTTP2Client();
        h2Client.setSelectors(1);
        HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

        jettyH2cClient = new HttpClient(transport);
        jettyH2cClient.start();
        jettyFeignH2c = JettyReactiveFeign.<FeignReactorTestInterface>builder()
                .options(new JettyReactiveOptions.Builder().setUseHttp2(true).build())
                .target(FeignReactorTestInterface.class, SERVER_H2C_URL);
        //to settle TCP connection
        jettyFeignH2c.postWithPayload(Mono.just(Collections.emptyMap())).block();

        java11Feign = Java11ReactiveFeign.<FeignReactorTestInterface>builder()
                .target(FeignReactorTestInterface.class, SERVER_URL);

        java11FeignH2c = Java11ReactiveFeign.<FeignReactorTestInterface>builder()
                .options(new Java11ReactiveOptions.Builder().setUseHttp2(true).build())
                .target(FeignReactorTestInterface.class, SERVER_H2C_URL);
        //to settle TCP connection
        java11FeignH2c.postWithPayload(Mono.just(Collections.emptyMap())).block();

        feign = Feign.builder()
                .encoder((o, type, requestTemplate) -> {
                    try {
                        requestTemplate.body(objectMapper.writeValueAsString(o));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .decoder((response, type) ->
                        type == String.class
                                ? Util.toString(response.body().asReader(StandardCharsets.UTF_8))
                                : objectMapper.readValue(
                                response.body().asInputStream(),
                                (Class)((ParameterizedType)type).getRawType()))
                .target(FeignTestInterface.class, SERVER_URL);
    }

    protected void tearDown() throws Exception {
//        serverRx.shutdown();
        //H2C
        serverJettyH2c.stop();
//        serverWireMockH2c.shutdown();

        ((QueuedThreadPool)jettyHttpClient.getExecutor()).stop();
        jettyHttpClient.stop();
        jettyH2cClient.stop();
    }

    private HttpServer<ByteBuf, ByteBuf> rxNetty(int port){
        return RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            public rx.Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                              HttpServerResponse<ByteBuf> response) {
                if(request.getPath().equals(PATH_WITH_PAYLOAD)){
                    response.getHeaders().add("Content-Type", "application/json");
                    response.writeBytes(responseJson);
                }

                return response.flush();
            }
        });
    }

    private Server jettyH2c(int port){
        Server serverJetty = new Server();

        serverJetty.setHandler(new AbstractHandler(){
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                request.getInputStream().skip(Integer.MAX_VALUE);
                if(target.equals(PATH_WITH_PAYLOAD)){
                    response.addHeader("Content-Type", "application/json");
                    response.getOutputStream().write(responseJson);
                    response.getOutputStream().flush();
                }
                baseRequest.setHandled(true);
            }
        });

        //setup h2c
        HttpConfiguration httpConfig = new HttpConfiguration();
        ServerConnector connectorH2c = new ServerConnector(serverJetty,
                new HttpConnectionFactory(httpConfig),
                new HTTP2CServerConnectionFactory(httpConfig)
                );
        connectorH2c.setPort(port);
        serverJetty.addConnector(connectorH2c);

        return serverJetty;
    }

//    private WireMockServer wireMockH2c(int port){
//        WireMockServer server = new WireMockServer(wireMockConfig()
//                .port(port)
//                .asynchronousResponseEnabled(true));
//
//        server.stubFor(get(urlEqualTo("/"))
//                .willReturn(aResponse().withStatus(200)));
//
//        server.stubFor(post(urlEqualTo(PATH_WITH_PAYLOAD))
//                .willReturn(aResponse().withStatus(200)
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(responseJson)));
//        return server;
//    }

//    private WireMockConfiguration wireMockConfig(){
//        return WireMockConfiguration.wireMockConfig()
//                .httpServerFactory((options, adminRequestHandler, stubRequestHandler) ->
//                        new JettyHttpServer(options, adminRequestHandler, stubRequestHandler) {
//                            @Override
//                            protected ServerConnector createHttpConnector(
//                                    String bindAddress,
//                                    int port,
//                                    JettySettings jettySettings,
//                                    NetworkTrafficListener listener) {
//
//                                HttpConfiguration httpConfig = createHttpConfig(jettySettings);
//
//                                return createServerConnector(
//                                        bindAddress,
//                                        jettySettings,
//                                        port,
//                                        listener,
//                                        new HttpConnectionFactory(httpConfig),
//                                        new HTTP2CServerConnectionFactory(httpConfig)
//                                );
//                            }
//                        });
//    }
}
