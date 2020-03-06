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
package reactivefeign.java11.h1;

import com.fasterxml.jackson.core.JsonProcessingException;
import reactivefeign.ReactiveFeign;
import reactivefeign.java11.Java11ReactiveFeign;
import reactivefeign.java11.Java11ReactiveOptions;
import reactivefeign.testcase.IcecreamServiceApi;

import java.util.List;
import java.util.stream.Collectors;

import static reactivefeign.TestUtils.MAPPER;

/**
 * @author Sergii Karpenko
 */
public class LoggerTest extends reactivefeign.LoggerTest<LoggerTest.IcecreamServiceApiJava11H1> {

    @Override
    protected String appenderPrefix(){
        return "h1_";
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJava11H1> builder() {
        return Java11ReactiveFeign.builder();
    }

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApiJava11H1> builder(long readTimeoutInMillis) {
        return Java11ReactiveFeign.<IcecreamServiceApiJava11H1>builder().options(
                new Java11ReactiveOptions.Builder().setRequestTimeoutMillis(readTimeoutInMillis).build());
    }

    @Override
    protected String fluxRequestBody(List<?> list) {
        return list.stream()
                .map(element -> {
                    try {
                        return MAPPER.writeValueAsString(element);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.joining("\n"))+"\n";
    }

    @Override
    protected Class<IcecreamServiceApiJava11H1> target(){
        return IcecreamServiceApiJava11H1.class;
    }

    interface IcecreamServiceApiJava11H1 extends IcecreamServiceApi{}
}
