package reactivefeign.jetty.h2c;

import reactivefeign.ReactiveFeign;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;

public class TestUtils {

    public static <T> ReactiveFeign.Builder<T> builderHttp2() {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithAcceptCompressed(boolean acceptCompressed) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setAcceptCompressed(acceptCompressed)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithConnectTimeout(long connectTimeout) {
        return JettyReactiveFeign.<T>builder().options(
                new JettyReactiveOptions.Builder()
                        .setConnectTimeoutMillis(connectTimeout)
                        .setUseHttp2(true).build());
    }

}
