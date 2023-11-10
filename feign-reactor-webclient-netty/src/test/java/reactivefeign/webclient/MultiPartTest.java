package reactivefeign.webclient;

import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.webclient.netty.NettyWebReactiveFeign;

public class MultiPartTest extends reactivefeign.MultiPartTest {

    @Override
    protected ReactiveFeignBuilder<reactivefeign.MultiPartTest.MultipartClient> builder() {
        return NettyWebReactiveFeign.builder();
    }

}
