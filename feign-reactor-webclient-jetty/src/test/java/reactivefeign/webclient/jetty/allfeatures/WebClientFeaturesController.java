package reactivefeign.webclient.jetty.allfeatures;

import org.reactivestreams.Publisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class WebClientFeaturesController implements WebClientFeaturesApi {

    @PostMapping(path = "/mirrorStreamingBinaryBodyReactive")
    @Override
    public Flux<DataBuffer> mirrorStreamingBinaryBodyReactive(@RequestBody Publisher<DataBuffer> body) {
        return Flux.from(body);
    }

    @PostMapping(path = "/mirrorResourceReactiveWithZeroCopying")
    @Override
    public Flux<DataBuffer> mirrorResourceReactiveWithZeroCopying(@RequestBody Resource resource) {
        return DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 3);
    }
}
