package reactivefeign.webclient;

import reactivefeign.ReactiveFeignBuilder;

public class ResponseEntityTest extends reactivefeign.webclient.core.ResponseEntityTest{


    @Override
    protected <T> ReactiveFeignBuilder<T> builder() {
        return WebReactiveFeign.builder();
    }
}
