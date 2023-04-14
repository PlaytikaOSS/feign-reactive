package reactivefeign.webclient.client5.h1;

import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveOptions;
import reactivefeign.webclient.client5.HttpClient5ReactiveOptions;
import reactivefeign.webclient.client5.HttpClient5WebReactiveFeign;

public class TestUtils {

    public static <T> ReactiveFeign.Builder<T> builderHttp() {
        return HttpClient5WebReactiveFeign.builder();
    }

    static <T> ReactiveFeign.Builder<T> builderHttpWithAcceptCompressed(boolean acceptCompressed) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setAcceptCompressed(acceptCompressed)
                        .build()
        );
    }

    static <T> ReactiveFeign.Builder<T> builderHttpWithConnectTimeout(long connectTimeout) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setConnectTimeoutMillis(connectTimeout)
                        .build()
        );
    }

    static <T> ReactiveFeign.Builder<T> builderHttpWithSocketTimeout(long socketTimeout) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setSocketTimeout((int)socketTimeout)
                        .setConnectTimeoutMillis(socketTimeout / 2)
                        .build()
        );
    }

    static <T> ReactiveFeign.Builder<T> builderHttpFollowRedirects(boolean followRedirects) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setFollowRedirects(followRedirects)
                        .build()
        );
    }

    static <T> ReactiveFeign.Builder<T> builderHttpProxy(ReactiveOptions.ProxySettings proxySettings) {
        return HttpClient5WebReactiveFeign.<T>builder().options(
                new HttpClient5ReactiveOptions.Builder()
                        .setProxySettings(proxySettings)
                        .build()
        );
    }

}
