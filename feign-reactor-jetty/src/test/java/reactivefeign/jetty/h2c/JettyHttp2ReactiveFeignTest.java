package reactivefeign.jetty.h2c;

import org.eclipse.jetty.client.HttpClient;
import org.junit.Test;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;

public class JettyHttp2ReactiveFeignTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNotHttp2Transport(){
        JettyReactiveFeign.builder(new HttpClient())
                .options(new JettyReactiveOptions.Builder().setUseHttp2(true).build())
                .build();
    }

}
