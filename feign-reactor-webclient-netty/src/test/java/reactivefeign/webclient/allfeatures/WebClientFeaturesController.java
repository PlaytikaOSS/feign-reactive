package reactivefeign.webclient.allfeatures;

import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactivefeign.allfeatures.AllFeaturesApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class WebClientFeaturesController {

    @PostMapping(path = "/mirrorStreamingBinaryBodyReactive")
    public Flux<DataBuffer> mirrorStreamingBinaryBodyReactive(@RequestBody Publisher<DataBuffer> body) {
        return Flux.from(body);
    }

    @PostMapping(path = "/mirrorResourceReactiveWithZeroCopying")
    public Flux<DataBuffer> mirrorResourceReactiveWithZeroCopying(@RequestBody Resource resource) {
        return DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 3);
    }

    @PostMapping(path = "/passUrlEncodedForm",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public Mono<AllFeaturesApi.TestObject> passUrlEncodedForm(ServerWebExchange serverWebExchange){
        return serverWebExchange.getFormData()
                .map(formData -> new AllFeaturesApi.TestObject(formData.toString()));
    }
}
