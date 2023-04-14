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
package reactivefeign.webclient.client5.h1;

import org.junit.Ignore;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.testcase.IcecreamServiceApi;

import static reactivefeign.webclient.client5.h1.TestUtils.builderHttp;
import static reactivefeign.webclient.client5.h1.TestUtils.builderHttpWithSocketTimeout;

/**
 * @author Sergii Karpenko
 */
public class LoggerTest extends reactivefeign.LoggerTest<LoggerTest.IcecreamServiceApiJettyH1> {

    //TODO investigate why socket timeout doesn't work
    @Ignore
    @Override
    @Test
    public void shouldLogTimeout() {
    }

    @Override
    protected String appenderPrefix(){
        return "h1_";
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJettyH1> builder() {
        return builderHttp();
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJettyH1> builder(long readTimeoutInMillis) {
        return builderHttpWithSocketTimeout(readTimeoutInMillis);
    }

    @Override
    protected Class<IcecreamServiceApiJettyH1> target(){
        return IcecreamServiceApiJettyH1.class;
    }

    interface IcecreamServiceApiJettyH1 extends IcecreamServiceApi{}
}
