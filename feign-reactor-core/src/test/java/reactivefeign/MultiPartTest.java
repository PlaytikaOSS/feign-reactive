package reactivefeign;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {"spring.main.web-application-type=reactive"},
        classes = {MultiPartTest.TestController.class, MultiPartTest.TestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
abstract public class MultiPartTest extends BaseReactorTest {

    private MultipartClient client;

    @LocalServerPort
    protected int port;

    abstract protected ReactiveFeignBuilder<MultipartClient> builder();

    @Before
    public void setUp() {
        client = builder()
                .decode404()
                .target(MultipartClient.class, "http://localhost:" + port);
    }

    @Test
    public void shouldProcessSingleMultipart() {
        final Flux<Part> parts = Flux.just(
                new TestPart("files", "content1"),
                new TestPart("filesIgnoredOnServer", "contentIgnoredOnServer")
        );

        final Map<String, List<ByteBuffer>> returned = client.multipart(parts)
                .subscribeOn(testScheduler()).block();

        assertThat(returned.size()).isEqualTo(1);
        assertThat(returned.get("files")).isNotNull();
        assertThat(returned.get("files").size()).isEqualTo(1);
        assertByteBuffer(returned.get("files").get(0), "content1");

    }

    @Test
    public void shouldProcessSingleMultipartBody() {
        final Flux<Part> parts = Flux.just(
                new TestPart("files1", "content1")
        );

        final Map<String, List<ByteBuffer>> returned = client.multipartBody(parts)
                .subscribeOn(testScheduler()).block();

        assertThat(returned.size()).isEqualTo(1);
        assertThat(returned.get("files1")).isNotNull();
        assertThat(returned.get("files1").size()).isEqualTo(1);
        assertByteBuffer(returned.get("files1").get(0), "content1");
    }

    @Test
    public void shouldProcessMultipleMultipartBody() {
        final Flux<Part> parts = Flux.just(
                new TestPart("files1", "content1"),
                new TestPart("files2", "content2")
        );

        final Map<String, List<ByteBuffer>> returned = client.multipartBody(parts)
                .subscribeOn(testScheduler()).block();

        assertThat(returned.size()).isEqualTo(2);
        assertThat(returned.get("files1")).isNotNull();
        assertThat(returned.get("files1").size()).isEqualTo(1);
        assertByteBuffer(returned.get("files1").get(0), "content1");
        assertThat(returned.get("files2")).isNotNull();
        assertThat(returned.get("files2").size()).isEqualTo(1);
        assertByteBuffer(returned.get("files2").get(0), "content2");
    }

    @Test
    public void shouldProcessMixedMultipartBody() {

        final Map<String, String> returned = client.multipartMixed(
                        "testOrg", 1,
                        new ByteArrayResource("content1".getBytes(), "files1"))
                .subscribeOn(testScheduler()).block();

        assertThat(returned.size()).isEqualTo(3);
        assertThat(returned.get("organization.content")).isNotNull();
        assertThat(returned.get("organization.name")).isEqualTo("testOrg");
        assertThat(returned.get("organization.id")).isEqualTo("1");
        assertThat(returned.get("organization.content")).isEqualTo("content1");
    }

    private void assertByteBuffer(ByteBuffer actual, String expected) {
        byte[] dataReceived = new byte[actual.limit()];
        actual.get(dataReceived);
        assertThat(new String(dataReceived)).isEqualTo(expected);
    }


    public interface MultipartClient {
        @RequestLine("POST " + "/multipart")
        @Headers({"Content-Type: " + MULTIPART_FORM_DATA_VALUE,
                "Accept: " + APPLICATION_JSON_VALUE})
        Mono<Map<String, List<ByteBuffer>>> multipart(Flux<Part> files);

        @RequestLine("POST " + "/multipartBody")
        @Headers({"Content-Type: " + MULTIPART_FORM_DATA_VALUE,
                "Accept: " + APPLICATION_JSON_VALUE})
        Mono<Map<String, List<ByteBuffer>>> multipartBody(Flux<Part> files);

        @RequestLine("POST " + "/multipartMixed")
        @Headers({"Content-Type: " + MULTIPART_FORM_DATA_VALUE,
                "Accept: " + APPLICATION_JSON_VALUE})
        Mono<Map<String, String>> multipartMixed(
                @Param("organization.name") String organizationName,
                @Param("organization.id") Integer organizationId,
                @Param("organization.content") Resource part);
    }


    @RestController
    public static class TestController {
        @PostMapping(path = "/multipart")
        public Mono<Map<String, List<ByteBuffer>>> multipart(@RequestPart("files") Flux<Part> parts) {
            return multipartBody(parts);
        }

        @PostMapping(path = "/multipartBody")
        public Mono<Map<String, List<ByteBuffer>>> multipartBody(@RequestBody Flux<Part> parts) {
            return parts.collectMap(Part::name, MultiPartTest::toByteBuffers);
        }

        @PostMapping(path = "/multipartMixed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Mono<Map<String, String>> multipartMixed(
                @RequestPart("organization.name") String organizationName,
                @RequestPart("organization.id") Integer organizationId,
                @RequestPart("organization.content") Part part) {
            return Mono.just(new HashMap<String, String>(){{
                put("organization.name", organizationName);
                put("organization.id", organizationId.toString());
                put(part.name(), partToString(part));
            }});
        }
    }

    private static String partToString(Part part) {
        ByteBuffer byteBuffer = toByteBuffers(part).get(0);
        byte[] arr = new byte[byteBuffer.remaining()];
        byteBuffer.get(arr);
        return new String(arr);
    }

    private static List<ByteBuffer> toByteBuffers(Part part) {
        return part.content().map(DataBuffer::asByteBuffer).collectList()
                .share().block();
    }

    @Configuration
    @Profile("netty")
    public static class TestConfiguration {
        @Bean
        public ReactiveWebServerFactory reactiveWebServerFactory() {
            return new NettyReactiveWebServerFactory();
        }
    }


    private static class TestPart implements Part {
        private final String name;
        private final String content;

        TestPart(String name, String content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HttpHeaders headers() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            return headers;
        }

        @Override
        public Flux<DataBuffer> content() {
            DataBufferFactory factory = new DefaultDataBufferFactory();
            DataBuffer dataBuffer = factory.allocateBuffer().write(content.getBytes());
            return Flux.just(dataBuffer);
        }
    }

}
