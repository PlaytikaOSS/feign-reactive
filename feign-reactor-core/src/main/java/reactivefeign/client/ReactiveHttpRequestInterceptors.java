package reactivefeign.client;

import reactivefeign.utils.Pair;

import java.util.List;

import static reactivefeign.utils.MultiValueMapUtils.addOrdered;

public final class ReactiveHttpRequestInterceptors {

    private ReactiveHttpRequestInterceptors(){}

    public static ReactiveHttpRequestInterceptor addHeaders(List<Pair<String, String>> headers){
        return request -> {
            headers.forEach(header -> addOrdered(request.headers(), header.left, header.right));
            return request;
        };
    }

    public static ReactiveHttpRequestInterceptor composite(List<ReactiveHttpRequestInterceptor> interceptors){
        return request -> {
            ReactiveHttpRequest result = request;
            for(ReactiveHttpRequestInterceptor interceptor : interceptors){
                result = interceptor.apply(result);
            }
            return result;
        };
    }

}
