package reactivefeign.webclient.jetty;

import reactivefeign.ReactiveFeignBuilder;

public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<MultipartClient> builder() {
        return JettyWebReactiveFeign.builder();
    }

}
