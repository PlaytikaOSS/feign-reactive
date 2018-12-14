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
package reactivefeign.jetty;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServer;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.ServerConnector;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;

import java.util.function.Predicate;

/**
 * @author Sergii Karpenko
 */
public class SmokeTest extends reactivefeign.SmokeTest {

  @Override
  protected WireMockConfiguration wireMockConfig(){
    return JettyHttp2cServerConfig.wireMockConfig();
  }

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
    return JettyHttp2ReactiveFeign.builder();
  }

  @Override
  protected Predicate<Throwable> corruptedJsonError() {
    return throwable -> throwable instanceof JsonEOFException;
  }
}
