package reactivefeign.client;

import feign.template.UriUtils;
import reactivefeign.utils.Pair;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static reactivefeign.utils.MultiValueMapUtils.addOrdered;

public final class ReactiveHttpRequestInterceptors {
    private static final String QUERY_PAIRS_SEPARATOR = "&";
    private static final String QUERY_KEY_VALUE_SEPARATOR = "=";

    private ReactiveHttpRequestInterceptors() {
    }

    public static ReactiveHttpRequestInterceptor addHeader(String header, String value) {
        return addHeaders(Collections.singletonList(new Pair<>(header, value)));
    }

    public static ReactiveHttpRequestInterceptor addHeaders(List<Pair<String, String>> headers) {
        return from(request -> {
            headers.forEach(header -> addOrdered(request.headers(), header.left, header.right));
            return request;
        });
    }

    public static ReactiveHttpRequestInterceptor addQuery(String name, String value) {
        return addQueries(Collections.singletonList(new Pair<>(name, value)));
    }

    public static ReactiveHttpRequestInterceptor addQueries(List<Pair<String, String>> queries) {
        return ReactiveHttpRequestInterceptors.from(reactiveHttpRequest
                -> reactiveHttpRequestWithQueries(reactiveHttpRequest, queries));
    }

    public static ReactiveHttpRequestInterceptor from(Function<ReactiveHttpRequest, ReactiveHttpRequest> function) {
        return request -> Mono.just(function.apply(request));
    }

    private static ReactiveHttpRequest reactiveHttpRequestWithQueries(ReactiveHttpRequest reactiveHttpRequest, List<Pair<String, String>> queries) {
        URI uri = reactiveHttpRequest.uri();
        String query = uri.getQuery();
        for(Pair<String, String> queryPair : queries) {
            String keyValuePair = queryPair.left + QUERY_KEY_VALUE_SEPARATOR + queryPair.right;

            if (query == null) {
                query = keyValuePair;
            } else {
                query += QUERY_PAIRS_SEPARATOR + keyValuePair;
            }
        }

        try {
            return new ReactiveHttpRequest(reactiveHttpRequest, new URI(uri.getScheme(), uri.getAuthority(),
                    uri.getPath(), query, uri.getFragment()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
