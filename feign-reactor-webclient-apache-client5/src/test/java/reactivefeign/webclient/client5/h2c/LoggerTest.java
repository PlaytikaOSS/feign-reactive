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
package reactivefeign.webclient.client5.h2c;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;

import static reactivefeign.webclient.client5.h2c.TestUtils.builderHttp2;
import static reactivefeign.webclient.client5.h2c.TestUtils.builderHttp2WithRequestTimeout;
import static reactivefeign.wiremock.WireMockServerConfigurations.h2cConfig;

/**
 * @author Sergii Karpenko
 */
public class LoggerTest extends reactivefeign.LoggerTest<LoggerTest.IcecreamServiceApiJettyH2> {

    //TODO investigate why socket timeout doesn't work
    @Ignore
    @Override
    @Test
    public void shouldLogTimeout() {
    }

    @Override
    protected String appenderPrefix(){
        return "h2c_";
    }

    @Override
    protected WireMockConfiguration wireMockConfig(){
        return h2cConfig();
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJettyH2> builder() {
        return builderHttp2();
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJettyH2> builder(long readTimeoutInMillis) {
        return builderHttp2WithRequestTimeout(readTimeoutInMillis);
    }

    @Override
    protected Class<IcecreamServiceApiJettyH2> target(){
        return IcecreamServiceApiJettyH2.class;
    }

    interface IcecreamServiceApiJettyH2 extends IcecreamServiceApi{}
}
