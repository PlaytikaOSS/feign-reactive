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
package reactivefeign.webclient.jetty;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactivefeign.ReactiveFeign;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.testcase.IcecreamServiceApi;

import java.net.ConnectException;

/**
 * @author Sergii Karpenko
 */
public class ConnectionTimeoutTest extends reactivefeign.ConnectionTimeoutTest {

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(long connectTimeoutInMillis) {
    return JettyWebReactiveFeign.<IcecreamServiceApi>builder().options(
            new JettyReactiveOptions.Builder().setConnectTimeoutMillis(connectTimeoutInMillis).build()
    );
  }

  @Override
  protected boolean isConnectException(Throwable throwable){
    return throwable instanceof ReactiveFeignException
            && throwable.getCause() instanceof WebClientRequestException
            && throwable.getCause().getCause() instanceof ConnectException;
  }
}
