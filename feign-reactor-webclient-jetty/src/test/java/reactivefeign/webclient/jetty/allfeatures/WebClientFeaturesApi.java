package reactivefeign.webclient.jetty.allfeatures;

import feign.Headers;
import feign.RequestLine;
import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

public interface WebClientFeaturesApi {

    @RequestLine("POST " + "/mirrorStreamingBinaryBodyReactive")
    @Headers({ "Content-Type: "+APPLICATION_OCTET_STREAM_VALUE })
    Flux<DataBuffer> mirrorStreamingBinaryBodyReactive(Publisher<DataBuffer> body);

    @RequestLine("POST " + "/mirrorResourceReactiveWithZeroCopying")
    @Headers({ "Content-Type: "+APPLICATION_OCTET_STREAM_VALUE })
    Flux<DataBuffer> mirrorResourceReactiveWithZeroCopying(Resource resource);
}
