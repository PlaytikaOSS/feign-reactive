package reactivefeign.jetty.h1;

import org.junit.jupiter.api.Disabled;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.jetty.JettyReactiveFeign;

// JettyReactiveFeign client does not yet implement multipart encoding
@Disabled
public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<MultipartClient> builder() {
        return JettyReactiveFeign.builder();
    }

}
