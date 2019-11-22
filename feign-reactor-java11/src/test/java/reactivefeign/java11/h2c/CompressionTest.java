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
package reactivefeign.java11.h2c;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;

import static reactivefeign.ReactivityTest.CALLS_NUMBER;
import static reactivefeign.java11.h2c.TestUtils.builderHttp2WithAcceptCompressed;
import static reactivefeign.wiremock.WireMockServerConfigurations.h2cConfig;

/**
 * @author Sergii Karpenko
 */
public class CompressionTest extends reactivefeign.CompressionTest {

  @Override
  protected WireMockConfiguration wireMockConfig(){
    return h2cConfig(true, CALLS_NUMBER);
  }

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApi> builder(boolean tryUseCompression) {
    return builderHttp2WithAcceptCompressed(tryUseCompression);
  }

  //TODO implement reactive gzip decoder
  @Test(expected = java.lang.AssertionError.class)
  public void testCompression() throws JsonProcessingException {
    super.testCompression();
  }
}
