package reactivefeign.jetty.h1;

import reactivefeign.ReactiveFeign;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;

public class TestUtils {

    public static final int DEFAULT_REQUEST_TIMEOUT = 1000;

    public static <T> ReactiveFeign.Builder<T> builderHttp() {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttpWithAcceptCompressed(boolean acceptCompressed) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT)
                        .setAcceptCompressed(acceptCompressed).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttpWithConnectTimeout(long connectTimeout) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT)
                        .setConnectTimeoutMillis(connectTimeout).build());
    }

}
