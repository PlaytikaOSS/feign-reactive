package reactivefeign.java11;

import java.net.http.HttpClient;
import java.util.function.Consumer;

public interface HttpClientFeignCustomizer extends Consumer<HttpClient.Builder> {
}
