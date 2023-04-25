package reactivefeign.webclient.client5.h1;

import reactivefeign.ReactiveFeignBuilder;

import static reactivefeign.webclient.client5.h1.TestUtils.builderHttp;

public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<MultipartClient> builder() {
        return builderHttp();
    }

}
