package reactivefeign.benchmarks;

import feign.Feign;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.webclient.WebReactiveFeign;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

abstract public class RealRequestBenchmarks {

    public static final int SERVER_PORT = 8766;
    private HttpServer<ByteBuf, ByteBuf> server;

    protected FeignReactorTestInterface webClientFeign;

    protected HttpClient jettyHttpClient;
    protected FeignReactorTestInterface jettyFeign;

    protected FeignTestInterface feign;

    protected void setup() throws Exception {

        //just OK response with empty body on any request
        server = RxNetty.createHttpServer(SERVER_PORT, new RequestHandler<ByteBuf, ByteBuf>() {
            public rx.Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                              HttpServerResponse<ByteBuf> response) {
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

        feign = Feign.builder().target(FeignTestInterface.class, "http://localhost:" + SERVER_PORT);
    }

    protected void tearDown() throws Exception {
        server.shutdown();
        ((QueuedThreadPool)jettyHttpClient.getExecutor()).stop();
        jettyHttpClient.stop();
    }

}
