package reactivefeign.java11;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Target;
import org.junit.Test;
import reactivefeign.java11.client.Java11ReactiveHttpClientFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class Java11ReactiveHttpClientFactoryTest {

    private HttpClient httpClient = mock(HttpClient.class);

    private Java11ReactiveHttpClientFactory reactiveHttpClient = new Java11ReactiveHttpClientFactory(
            httpClient, new JsonFactory(), new ObjectMapper(),
            new Java11ReactiveOptions.Builder().build(), HttpClient.Version.HTTP_2);

    @Test
    public void shouldMakePreliminaryCallToUpgradeToHttp2() throws IOException, InterruptedException {
        Target mock = mock(Target.class);
        when(mock.url()).thenReturn("http://test.url");
        reactiveHttpClient.target(mock);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

}
