package reactivefeign.webclient;

import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

public interface WebClientFeignCustomizer extends Consumer<WebClient.Builder> {
}
