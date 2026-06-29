package reactivefeign.jetty.h2c;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Test;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JettyHttp2ReactiveFeignTest {

    @Test
    public void shouldFailIfNotHttp2Transport() {
      assertThrows(IllegalArgumentException.class, () ->
        JettyReactiveFeign.builder(new HttpClient())
                .options(new JettyReactiveOptions.Builder().setUseHttp2(true).build())
                .build());
    }

}
