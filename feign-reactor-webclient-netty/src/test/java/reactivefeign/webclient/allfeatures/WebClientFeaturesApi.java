package reactivefeign.webclient.allfeatures;

import feign.Headers;
import feign.RequestLine;
import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MultiValueMap;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

public interface WebClientFeaturesApi {

    @Headers({ "Content-Type: "+APPLICATION_OCTET_STREAM_VALUE })
    @RequestLine("POST " + "/mirrorStreamingBinaryBodyReactive")
    Flux<DataBuffer> mirrorStreamingBinaryBodyReactive(Publisher<DataBuffer> body);

    @Headers({ "Content-Type: "+APPLICATION_OCTET_STREAM_VALUE })
    @RequestLine("POST " + "/mirrorResourceReactiveWithZeroCopying")
    Flux<DataBuffer> mirrorResourceReactiveWithZeroCopying(Resource resource);

    @Headers({ "Content-Type: "+APPLICATION_FORM_URLENCODED_VALUE })
    @RequestLine("POST /passUrlEncodedForm")
    Mono<AllFeaturesApi.TestObject> passUrlEncodedForm(MultiValueMap<String, String> form);
}
