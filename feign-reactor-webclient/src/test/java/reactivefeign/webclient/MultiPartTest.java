package reactivefeign.webclient;

import reactivefeign.ReactiveFeignBuilder;

public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<reactivefeign.MultiPartTest.MultipartClient> builder() {
        return WebReactiveFeign.builder();
    }

}
