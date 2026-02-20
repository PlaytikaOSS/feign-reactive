package reactivefeign.webclient;

import reactor.netty.http.client.HttpClient;

import java.util.function.Function;

public interface NettyHttpClientCustomizer extends Function<HttpClient, HttpClient> {
}
