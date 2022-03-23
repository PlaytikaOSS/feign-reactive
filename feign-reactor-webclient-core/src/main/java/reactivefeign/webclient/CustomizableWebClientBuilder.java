package reactivefeign.webclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.PartHttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilderFactory;

import java.util.*;
import java.util.function.Consumer;


/**
 * The main purpose of this class is to protect properties that already have been set by customizer and do not overwrite them
 */
public class CustomizableWebClientBuilder implements WebClient.Builder {

    private static final Logger logger = LoggerFactory.getLogger(CustomizableWebClientBuilder.class);

    private final WebClient.Builder builder;

    private String baseUrl;
    private Map<String, ?> defaultUriVariables;
    private UriBuilderFactory uriBuilderFactory;
    private Consumer<HttpHeaders> headersConsumer;
    private Map<String, String[]> headers = new HashMap<>();
    private Consumer<MultiValueMap<String, String>> cookiesConsumer;
    private Map<String, String[]> cookies = new HashMap<>();
    private Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest;
    private Consumer<List<ExchangeFilterFunction>> filtersConsumer;
    private ExchangeStrategies strategies;
    private ExchangeFunction exchangeFunction;
    private ClientHttpConnector connector;
    private Consumer<WebClient.Builder> builderConsumer;
    private List<ExchangeFilterFunction> filters = new ArrayList<>();
    private Consumer<ClientCodecConfigurer> consumer;
    private Consumer<ExchangeStrategies.Builder> exchangeStrategies;

    private static final List<MediaType> MULTIPART_MEDIA_TYPES = Arrays.asList(
    MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_MIXED, MediaType.MULTIPART_RELATED);

    public CustomizableWebClientBuilder(WebClient.Builder builder) {
        //PartHttpMessageWriter missed in default codecs
        builder = addMultipartCodec(builder);
        this.builder = builder;
    }

    public WebClient.Builder addMultipartCodec(WebClient.Builder builder) {
        return builder.codecs(clientCodecConfigurer -> clientCodecConfigurer.customCodecs().register(
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
                }));
    }

    @Override
    public WebClient.Builder baseUrl(String baseUrl) {
        if(this.baseUrl == null){
            this.baseUrl = baseUrl;
        } else {
            logger.warn("Will ignore baseUrl parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
        if(this.defaultUriVariables == null){
            this.defaultUriVariables = defaultUriVariables;
        } else {
            logger.warn("Will ignore defaultUriVariables parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
        if(this.uriBuilderFactory == null){
            this.uriBuilderFactory = uriBuilderFactory;
        } else {
            logger.warn("Will ignore uriBuilderFactory parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultHeader(String header, String... values) {
        if(headers.putIfAbsent(header, values) != null){
            logger.warn("Will ignore header parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
        if(this.headersConsumer == null){
            this.headersConsumer = headersConsumer;
        } else {
            logger.warn("Will ignore headersConsumer parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultCookie(String cookie, String... values) {
        if(cookies.putIfAbsent(cookie, values) != null){
            logger.warn("Will ignore cookie parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
        if(this.cookiesConsumer == null){
            this.cookiesConsumer = cookiesConsumer;
        } else {
            logger.warn("Will ignore cookiesConsumer parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder defaultRequest(Consumer<WebClient.RequestHeadersSpec<?>> defaultRequest) {
        if(this.defaultRequest == null){
            this.defaultRequest = defaultRequest;
        } else {
            logger.warn("Will ignore defaultRequest parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder filter(ExchangeFilterFunction filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public WebClient.Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer) {
        if(this.filtersConsumer == null){
            this.filtersConsumer = filtersConsumer;
        } else {
            logger.warn("Will ignore filtersConsumer parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder clientConnector(ClientHttpConnector connector) {
        if(this.connector != null){
            logger.warn("Will override connector parameter as it's already been set");
        }
        this.connector = connector;
        return this;
    }

    @Override
    public WebClient.Builder codecs(Consumer<ClientCodecConfigurer> consumer) {
        if (this.consumer == null) {
            this.consumer = consumer;
        } else {
            logger.warn("Will ignore consumer parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
        if(this.strategies == null){
            this.strategies = strategies;
        } else {
            logger.warn("Will ignore strategies parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> exchangeStrategies) {
        if (this.exchangeStrategies == null) {
            this.exchangeStrategies = exchangeStrategies;
        } else {
            logger.warn("Will ignore strategies parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder exchangeFunction(ExchangeFunction exchangeFunction) {
        if(this.exchangeFunction == null){
            this.exchangeFunction = exchangeFunction;
        } else {
            logger.warn("Will ignore exchangeFunction parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient.Builder clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebClient.Builder apply(Consumer<WebClient.Builder> builderConsumer) {
        if(this.builderConsumer == null){
            this.builderConsumer = builderConsumer;
        } else {
            logger.warn("Will ignore builderConsumer parameter as it's already been set");
        }
        return this;
    }

    @Override
    public WebClient build() {
        WebClient.Builder builder = this.builder;
        if(baseUrl != null){
            builder = builder.baseUrl(baseUrl);
        }
        if(defaultUriVariables != null){
            builder = builder.defaultUriVariables(defaultUriVariables);
        }
        if(uriBuilderFactory != null){
            builder = builder.uriBuilderFactory(uriBuilderFactory);
        }
        if(headersConsumer != null){
            builder = builder.defaultHeaders(headersConsumer);
        }
        for(Map.Entry<String, String[]> entry : headers.entrySet()){
            builder = builder.defaultHeader(entry.getKey(), entry.getValue());
        }
        if(cookiesConsumer != null){
            builder = builder.defaultCookies(cookiesConsumer);
        }
        for(Map.Entry<String, String[]> entry : cookies.entrySet()){
            builder = builder.defaultCookie(entry.getKey(), entry.getValue());
        }
        if(defaultRequest != null){
            builder = builder.defaultRequest(defaultRequest);
        }
        if(filtersConsumer != null){
            builder = builder.filters(filtersConsumer);
        }
        for(ExchangeFilterFunction filter : filters){
            builder = builder.filter(filter);
        }
        if(strategies != null){
            builder = builder.exchangeStrategies(strategies);
        }
        if(exchangeFunction != null){
            builder = builder.exchangeFunction(exchangeFunction);
        }
        if(connector != null){
            builder = builder.clientConnector(connector);
        }
        if(builderConsumer != null){
            builder = builder.apply(builderConsumer);
        }
        if(consumer != null) {
            builder = builder.codecs(consumer);
        }
        if(exchangeStrategies != null) {
            builder = builder.exchangeStrategies(exchangeStrategies);
        }

        return builder.build();
    }

}
