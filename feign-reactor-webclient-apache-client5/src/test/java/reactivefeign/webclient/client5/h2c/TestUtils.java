package reactivefeign.webclient.client5.h2c;

import reactivefeign.ReactiveFeign;
import reactivefeign.webclient.client5.HttpClient5ReactiveOptions;
import reactivefeign.webclient.client5.HttpClient5WebReactiveFeign;

public class TestUtils {

    public static final int DEFAULT_REQUEST_TIMEOUT = 1000;

    public static <T> ReactiveFeign.Builder<T> builderHttp2() {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT)
                        .setUseHttp2(true).build());
    }

    public static <T> ReactiveFeign.Builder<T> builderHttp2WithRequestTimeout(long requestTimeout) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setSocketTimeout((int)requestTimeout)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithAcceptCompressed(boolean acceptCompressed) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setAcceptCompressed(acceptCompressed)
                        .setUseHttp2(true)
                        .build()
        );
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2WithConnectTimeout(long connectTimeout) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT)
                        .setConnectTimeoutMillis(connectTimeout)
                        .setUseHttp2(true).build());
    }

    static <T> ReactiveFeign.Builder<T> builderHttp2FollowRedirects(boolean follow) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setSocketTimeout(DEFAULT_REQUEST_TIMEOUT)
                        .setFollowRedirects(follow)
                        .setUseHttp2(true).build());
    }

}
