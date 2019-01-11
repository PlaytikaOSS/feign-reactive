package reactivefeign.jetty;

import org.eclipse.jetty.client.HttpClient;
import org.junit.Test;

public class JettyHttp2ReactiveFeignTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNotHttp2Transport(){
        JettyHttp2ReactiveFeign.builder(new HttpClient());
    }

}
