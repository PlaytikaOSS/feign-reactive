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
package reactivefeign.jetty.h2c;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import reactivefeign.ReactiveFeign;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;
import reactivefeign.testcase.IcecreamServiceApi;

import static reactivefeign.wiremock.WireMockServerConfigurations.h2cConfig;

public class ReactivityTest extends reactivefeign.ReactivityTest {

  @Override
  protected WireMockConfiguration wireMockConfig(){
    return h2cConfig();
  }

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
    return JettyReactiveFeign.<IcecreamServiceApi>builder().options(
            new JettyReactiveOptions.Builder()
                    .setRequestTimeoutMillis(timeToCompleteReactively())
                    .setUseHttp2(true).build());
  }
}
