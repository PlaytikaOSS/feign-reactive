package reactivefeign.jetty.h1;

import org.junit.Ignore;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.jetty.JettyReactiveFeign;

@Ignore
//TODO add support for Jetty based
public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<MultipartClient> builder() {
        return JettyReactiveFeign.builder();
    }

}
