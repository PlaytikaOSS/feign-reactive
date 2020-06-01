package reactivefeign.java11.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.MethodMetadata;
import feign.Target;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.java11.Java11ReactiveOptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static reactivefeign.ReactiveOptions.useHttp2;

public class Java11ReactiveHttpClientFactory implements ReactiveHttpClientFactory {

    private final HttpClient httpClient;
    private final JsonFactory jsonFactory;
    private final ObjectMapper objectMapper;
    private final Java11ReactiveOptions options;

    public Java11ReactiveHttpClientFactory(
            HttpClient httpClient, JsonFactory jsonFactory, ObjectMapper objectMapper,
            Java11ReactiveOptions options) {
        this.httpClient = httpClient;
        this.jsonFactory = jsonFactory;
        this.objectMapper = objectMapper;
        this.options = options;

        if(useHttp2(options) && HttpClient.Version.HTTP_2 != httpClient.version()){
            throw new IllegalArgumentException("Set correct version to httpClient");
        }
    }

    @Override
    public void target(Target target) {
        if(httpClient.version() == HttpClient.Version.HTTP_2){
            //preliminary upgrade to h2s and setup TCP connection
            upgradeToH2c(target);
        }
    }

    @Override
    public ReactiveHttpClient create(MethodMetadata methodMetadata) {
        Java11ReactiveHttpClient reactiveHttpClient = Java11ReactiveHttpClient.java11Client(
                methodMetadata, httpClient, jsonFactory, objectMapper);
        if (options != null) {
            if(options.getRequestTimeoutMillis() != null) {
                reactiveHttpClient = reactiveHttpClient.setRequestTimeout(options.getRequestTimeoutMillis());
            }
            if(options.isTryUseCompression() != null) {
                reactiveHttpClient = reactiveHttpClient.setTryUseCompression(options.isTryUseCompression());
            }
        }

        return reactiveHttpClient;
    }

    private void upgradeToH2c(Target target){
        try {
            httpClient.send(HttpRequest.newBuilder()
                    .method("options", HttpRequest.BodyPublishers.noBody())
                    .uri(URI.create(target.url()))
                    .build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
