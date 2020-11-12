package reactivefeign.client;

import reactivefeign.utils.Pair;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
        return addHeaders(Collections.singletonList(new Pair<>(name, value)));
    }

    public static ReactiveHttpRequestInterceptor addQueries(List<Pair<String, String>> queries) {
        return ReactiveHttpRequestInterceptors.from(reactiveHttpRequest -> {
            for (Pair<String, String> query : queries) {
                reactiveHttpRequest = reactiveHttpRequestWithQuery(reactiveHttpRequest, query.left, query.right);
            }
            return reactiveHttpRequest;
        });
    }

    public static ReactiveHttpRequestInterceptor from(Function<ReactiveHttpRequest, ReactiveHttpRequest> function) {
        return request -> Mono.just(function.apply(request));
    }

    private static ReactiveHttpRequest reactiveHttpRequestWithQuery(ReactiveHttpRequest reactiveHttpRequest, String key, String value) {
        URI uri = reactiveHttpRequest.uri();
        String query = uri.getQuery();
        String keyValuePair = key + QUERY_KEY_VALUE_SEPARATOR + value;

        if (query == null) {
            query = keyValuePair;
        } else {
            query += QUERY_PAIRS_SEPARATOR + keyValuePair;
        }

        try {
            return new ReactiveHttpRequest(reactiveHttpRequest, new URI(uri.getScheme(), uri.getAuthority(),
                    uri.getPath(), query, uri.getFragment()));
        } catch (URISyntaxException e) {
            // Ignore error with malformed URL, cannot be sent here
            return reactiveHttpRequest;
        }
    }
}
