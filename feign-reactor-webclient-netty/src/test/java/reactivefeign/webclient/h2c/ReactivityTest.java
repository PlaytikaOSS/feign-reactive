/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.webclient.h2c;

import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.webclient.netty.NettyReactiveOptions;
import reactivefeign.webclient.netty.NettyWebReactiveFeign;
import reactor.netty.http.HttpProtocol;

import static reactor.netty.http.HttpProtocol.H2C;

public class ReactivityTest extends reactivefeign.ReactivityTest {

  @Override
  protected HttpProtocol[] serverProtocols(){
    return new HttpProtocol[]{H2C};
  }

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
    return builderHttp2();
  }
  private static NettyWebReactiveFeign.Builder<IcecreamServiceApi> builderHttp2() {
    return NettyWebReactiveFeign.<IcecreamServiceApi>builder()
            .options(new NettyReactiveOptions.Builder()
                    .setProtocols(new HttpProtocol[]{H2C})
                    .setConnectTimeoutMillis(1000)
                    .build());
  }

}
