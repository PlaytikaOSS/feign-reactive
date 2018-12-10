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
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.webclient.WebReactiveFeign;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import static reactivefeign.benchmarks.BenchmarkUtils.readJsonFromFile;
import static reactivefeign.benchmarks.BenchmarkUtils.readJsonFromFileAsBytes;

abstract public class RealRequestBenchmarks {

    public static final int SERVER_PORT = 8766;
    private HttpServer<ByteBuf, ByteBuf> server;

    protected FeignReactorTestInterface webClientFeign;

    protected HttpClient jettyHttpClient;
    protected FeignReactorTestInterface jettyFeign;

    protected FeignTestInterface feign;

    protected byte[] responseJson;
    protected Map<String, Object> requestPayload;
    protected ObjectMapper objectMapper;

    protected void setup() throws Exception {

        responseJson = readJsonFromFileAsBytes("/response.json");

        objectMapper = new ObjectMapper();
        requestPayload = objectMapper.readValue(readJsonFromFile("/request.json"), HashMap.class);

        server = RxNetty.createHttpServer(SERVER_PORT, new RequestHandler<ByteBuf, ByteBuf>() {
            public rx.Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                              HttpServerResponse<ByteBuf> response) {
                if(request.getPath().equals("/postWithPayload")){
                    response.getHeaders().add("Content-Type", "application/json");
                    response.writeBytes(responseJson);
                }

                return response.flush();
            }
        });
        server.start();

        webClientFeign = WebReactiveFeign.<FeignReactorTestInterface>builder()
                .target(FeignReactorTestInterface.class, "http://localhost:" + SERVER_PORT);

        jettyHttpClient = new HttpClient();
        jettyHttpClient.setMaxConnectionsPerDestination(10000);
        jettyHttpClient.start();
        jettyFeign = JettyReactiveFeign.<FeignReactorTestInterface>builder(jettyHttpClient)
                .target(FeignReactorTestInterface.class, "http://localhost:" + SERVER_PORT);

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
                                ? Util.toString(response.body().asReader())
                                : objectMapper.readValue(
                                response.body().asInputStream(),
                                (Class)((ParameterizedType)type).getRawType()))
                .target(FeignTestInterface.class, "http://localhost:" + SERVER_PORT);
    }

    protected void tearDown() throws Exception {
        server.shutdown();
        ((QueuedThreadPool)jettyHttpClient.getExecutor()).stop();
        jettyHttpClient.stop();
    }

}
