package reactivefeign.webclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.client.ReactiveHttpRequest;

import java.util.function.BiFunction;

import static reactivefeign.webclient.client.WebReactiveHttpClient.webClient;

abstract public class CoreWebBuilder<T> extends ReactiveFeign.Builder<T>{

    protected CustomizableWebClientBuilder webClientBuilder;

    protected CoreWebBuilder(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = new CustomizableWebClientBuilder(webClientBuilder);
        this.webClientBuilder.clientConnector(buildClientConnector());
        updateClientFactory();
    }

    protected CoreWebBuilder(WebClient.Builder webClientBuilder, WebClientFeignCustomizer webClientCustomizer) {
        this.webClientBuilder = new CustomizableWebClientBuilder(webClientBuilder);
        this.webClientBuilder.clientConnector(buildClientConnector());
        webClientCustomizer.accept(this.webClientBuilder);
        updateClientFactory();
    }

    @Override
    public ReactiveFeignBuilder<T> objectMapper(ObjectMapper objectMapper) {
        webClientBuilder.exchangeStrategies(builder -> builder.codecs(configurer -> {
            ClientCodecConfigurer.ClientDefaultCodecs clientDefaultCodecs = configurer.defaultCodecs();
            clientDefaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
            clientDefaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
        }));
        updateClientFactory();
        return this;
    }

    protected void updateClientFactory(){
        clientFactory(methodMetadata -> webClient(
                methodMetadata, webClientBuilder.build(), errorMapper()));
    }


    protected abstract BiFunction<ReactiveHttpRequest, Throwable, Throwable> errorMapper();

    protected abstract ClientHttpConnector buildClientConnector();
}
