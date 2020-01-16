package reactivefeign.spring.config;

import com.netflix.hystrix.exception.ExceptionNotWrappedByHystrix;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import feign.Response;
import reactor.core.Exceptions;

import static reactivefeign.utils.HttpUtils.StatusCodeFamily.CLIENT_ERROR;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.SERVER_ERROR;
import static reactivefeign.utils.HttpUtils.familyOf;

public class ErrorDecoder implements feign.codec.ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if(familyOf(response.status()) == CLIENT_ERROR){
            return new HystrixBadRequestException("will not trigger fallback and circuit breaker");
        } else if(familyOf(response.status()) == SERVER_ERROR){
            return new OriginalError();
        } else {
            throw Exceptions.propagate(new IllegalArgumentException("Unexpected response status"));
        }
    }

    public static class OriginalError extends RuntimeException implements ExceptionNotWrappedByHystrix {
    }
}
