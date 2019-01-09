package reactivefeign.java11.h2c;

import reactivefeign.ReactiveFeign;
import reactivefeign.java11.Java11ReactiveFeign;
import reactivefeign.java11.Java11ReactiveOptions;

public class TestUtils {

    public static <T> ReactiveFeign.Builder<T> builderHttp2() {
        return Java11ReactiveFeign.<T>builder().options(
                new Java11ReactiveOptions.Builder()
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithAcceptCompressed(boolean acceptCompressed) {
        return Java11ReactiveFeign.<T>builder().options(
                new Java11ReactiveOptions.Builder()
                        .setAcceptCompressed(acceptCompressed)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithConnectTimeout(long connectTimeout) {
        return Java11ReactiveFeign.<T>builder().options(
                new Java11ReactiveOptions.Builder()
                        .setConnectTimeoutMillis(connectTimeout)
                        .setUseHttp2(true).build());
    }

}
