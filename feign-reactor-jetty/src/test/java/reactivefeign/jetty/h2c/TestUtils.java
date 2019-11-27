package reactivefeign.jetty.h2c;

import reactivefeign.ReactiveFeign;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;

public class TestUtils {

    public static final int DEFAULT_REQUEST_TIMEOUT = 1000;

    public static <T> ReactiveFeign.Builder<T> builderHttp2() {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT)
                        .setUseHttp2(true).build());
    }

    public static <T> ReactiveFeign.Builder<T> builderHttp2WithRequestTimeout(long requestTimeout) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(requestTimeout)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithAcceptCompressed(boolean acceptCompressed) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT)
                        .setAcceptCompressed(acceptCompressed)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithConnectTimeout(long connectTimeout) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setRequestTimeoutMillis(DEFAULT_REQUEST_TIMEOUT)
                        .setConnectTimeoutMillis(connectTimeout)
                        .setUseHttp2(true).build());
    }

}
