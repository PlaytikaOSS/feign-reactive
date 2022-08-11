package reactivefeign.webclient.jetty;

import reactivefeign.ReactiveFeignBuilder;

public class ResponseEntityTest extends reactivefeign.webclient.core.ResponseEntityTest{


    @Override
    protected <T> ReactiveFeignBuilder<T> builder() {
        return JettyWebReactiveFeign.builder();
    }
}
