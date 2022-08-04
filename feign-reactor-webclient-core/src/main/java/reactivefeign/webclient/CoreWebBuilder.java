package reactivefeign.webclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.client.ReactiveHttpRequest;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static reactivefeign.webclient.client.WebReactiveHttpClient.webClient;

abstract public class CoreWebBuilder<T> extends ReactiveFeign.Builder<T>{

    private static final List<MediaType> MULTIPART_MEDIA_TYPES = Arrays.asList(
            MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED);

    protected WebClient.Builder webClientBuilder;
    protected WebClientFeignCustomizer webClientCustomizer;

    protected CoreWebBuilder(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;

        this.webClientBuilder.codecs(multipartCodec());
    }

    protected CoreWebBuilder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
        this(webClientBuilder);
        this.webClientCustomizer = webClientCustomizer;
    }

    @Override
    protected ReactiveHttpClientFactory clientFactory(){
        this.webClientBuilder.clientConnector(clientConnector());

        if(webClientCustomizer != null){
            webClientCustomizer.accept(webClientBuilder);
        }
        return methodMetadata -> webClient(
                methodMetadata, webClientBuilder.build(), errorMapper());
    }

    @Override
    public ReactiveFeignBuilder<T> objectMapper(ObjectMapper objectMapper) {
        webClientBuilder.codecs(codecsConfigurer -> {
            ClientCodecConfigurer.ClientDefaultCodecs clientDefaultCodecs = codecsConfigurer.defaultCodecs();
            clientDefaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            clientDefaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
        });
        return this;
    }

    protected abstract BiFunction<ReactiveHttpRequest, Throwable, Throwable> errorMapper();

    protected abstract ClientHttpConnector clientConnector();

    private Consumer<ClientCodecConfigurer> multipartCodec() {
        return clientCodecConfigurer -> clientCodecConfigurer.customCodecs().register(
                //fix PartHttpMessageWriter
                new PartHttpMessageWriter(){
                    @Override
                    public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
                        return isMediaTypeCompatible(mediaType)
                                && Part.class.isAssignableFrom(elementType.toClass());
                    }

                    public boolean isMediaTypeCompatible(@Nullable MediaType mediaType){
                        if (mediaType == null) {
                            return true;
                        }
                        for (MediaType supportedMediaType : MULTIPART_MEDIA_TYPES) {
                            if (supportedMediaType.isCompatibleWith(mediaType)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }
}
