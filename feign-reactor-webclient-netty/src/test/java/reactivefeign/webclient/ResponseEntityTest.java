package reactivefeign.webclient;

import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.webclient.netty.NettyWebReactiveFeign;

public class ResponseEntityTest extends reactivefeign.webclient.core.ResponseEntityTest{


    @Override
    protected <T> ReactiveFeignBuilder<T> builder() {
        return NettyWebReactiveFeign.builder();
    }
}
