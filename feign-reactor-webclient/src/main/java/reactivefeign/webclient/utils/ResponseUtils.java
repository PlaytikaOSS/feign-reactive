package reactivefeign.webclient.utils;

import org.reactivestreams.Publisher;
import org.springframework.http.ResponseEntity;
import reactivefeign.client.ReactiveHttpResponse;

import java.util.function.Function;

import static org.springframework.http.HttpStatus.resolve;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

public class ResponseUtils {

    public static <P extends Publisher<?>> Function<ReactiveHttpResponse<P>, ResponseEntity<P>> toResponseEntity(){
        return reactiveResponse -> new ResponseEntity<P>(
                reactiveResponse.body(),
                toMultiValueMap(reactiveResponse.headers()),
                resolve(reactiveResponse.status()));
    }

}
