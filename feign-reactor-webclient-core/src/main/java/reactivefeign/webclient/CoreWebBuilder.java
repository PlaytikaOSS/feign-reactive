package reactivefeign.webclient;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.client.ReactiveHttpRequest;

import java.util.Arrays;
import java.util.Collection;
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
            JsonMapper jsonMapper = toJsonMapper(objectMapper);
            ClientCodecConfigurer.ClientDefaultCodecs clientDefaultCodecs = codecsConfigurer.defaultCodecs();
            clientDefaultCodecs.jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
            clientDefaultCodecs.jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
        });
        return this;
    }

    protected abstract BiFunction<ReactiveHttpRequest, Throwable, Throwable> errorMapper();

    private JsonMapper toJsonMapper(ObjectMapper objectMapper) {
        if (objectMapper instanceof JsonMapper jsonMapper) {
            return jsonMapper;
        }

        JsonMapper.Builder builder = objectMapper.tokenStreamFactory() instanceof JsonFactory jsonFactory
                ? JsonMapper.builder(jsonFactory)
                : JsonMapper.builder();
        for (MapperFeature feature : MapperFeature.values()) {
            builder.configure(feature, objectMapper.isEnabled(feature));
        }
        for (SerializationFeature feature : SerializationFeature.values()) {
            builder.configure(feature, objectMapper.isEnabled(feature));
        }
        for (DeserializationFeature feature : DeserializationFeature.values()) {
            builder.configure(feature, objectMapper.isEnabled(feature));
        }
        builder.propertyNamingStrategy(objectMapper.serializationConfig().getPropertyNamingStrategy());
        builder.enumNamingStrategy(objectMapper.serializationConfig().getEnumNamingStrategy());
        builder.defaultDateFormat(objectMapper.serializationConfig().getDateFormat());
        builder.defaultLocale(objectMapper.serializationConfig().getLocale());
        builder.defaultTimeZone(objectMapper.serializationConfig().getTimeZone());
        builder.defaultBase64Variant(objectMapper.serializationConfig().getBase64Variant());
        builder.typeFactory(objectMapper.getTypeFactory());
        Collection<JacksonModule> registeredModules = objectMapper.registeredModules();
        if (!registeredModules.isEmpty()) {
            builder.addModules(registeredModules);
        }
        return builder.build();
    }

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
