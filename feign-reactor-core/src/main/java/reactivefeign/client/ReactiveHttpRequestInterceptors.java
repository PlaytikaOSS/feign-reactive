package reactivefeign.client;

import reactivefeign.utils.Pair;

import java.util.List;

import static reactivefeign.utils.MultiValueMapUtils.addOrdered;

public class ReactiveHttpRequestInterceptors {

    public static ReactiveHttpRequestInterceptor addHeaders(List<Pair<String, String>> headers){
        return request -> {
            headers.forEach(header -> addOrdered(request.headers(), header.left, header.right));
            return request;
        };
    }

}
