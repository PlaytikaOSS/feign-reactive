package reactivefeign.webclient.jetty.allfeatures;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactivefeign.webclient.jetty.JettyWebReactiveFeign;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static java.nio.ByteBuffer.wrap;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {"spring.main.web-application-type=reactive"},
        classes = {WebClientFeaturesController.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
public class WebClientFeaturesTest {

    private WebClientFeaturesApi client;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        client = JettyWebReactiveFeign.<WebClientFeaturesApi>builder()
                .decode404()
                .target(WebClientFeaturesApi.class, "http://localhost:" + port);
    }

    @Test
    public void shouldMirrorStreamingBinaryBodyReactive()  {

        Flux<DataBuffer> returned = client
                .mirrorStreamingBinaryBodyReactive(Flux.just(
                        fromByteArray(new byte[]{1,2,3}),
                        fromByteArray(new byte[]{4,5,6})));

        StepVerifier.create(returned)
                .expectNextMatches(dataBuffer -> dataBuffer.asByteBuffer().equals(wrap(new byte[]{1,2,3})))
                .expectNextMatches(dataBuffer -> dataBuffer.asByteBuffer().equals(wrap(new byte[]{4,5,6})))
                .verifyComplete();
    }

    private static DataBuffer fromByteArray(byte[] data){
        return new DefaultDataBufferFactory().wrap(data);
    }

    @Test
    public void shouldMirrorResourceReactiveWithZeroCopying(){
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteArrayResource resource = new ByteArrayResource(data);
        Flux<DataBuffer> returned = client.mirrorResourceReactiveWithZeroCopying(resource);
        assertThat(DataBufferUtils.join(returned).block().asByteBuffer()).isEqualTo(wrap(data));
    }


}
