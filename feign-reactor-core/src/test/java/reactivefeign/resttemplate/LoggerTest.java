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
package reactivefeign.resttemplate;

import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.resttemplate.client.RestTemplateFakeReactiveFeign;
import reactivefeign.resttemplate.client.RestTemplateReactiveOptions;
import reactivefeign.testcase.IcecreamServiceApi;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author Sergii Karpenko
 */
public class LoggerTest extends reactivefeign.LoggerTest<LoggerTest.IcecreamServiceApiRestTemplate> {

  @Override
  protected String appenderPrefix(){
    return "resttemplate_";
  }

  @Override
  protected ReactiveFeign.Builder<IcecreamServiceApiRestTemplate> builder() {
    return RestTemplateFakeReactiveFeign.builder();
  }

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApiRestTemplate> builder(long readTimeoutInMillis) {
    return RestTemplateFakeReactiveFeign.<IcecreamServiceApiRestTemplate>builder().options(
            new RestTemplateReactiveOptions.Builder().setReadTimeoutMillis(readTimeoutInMillis).build()
    );
  }

  @Override
  protected Class<IcecreamServiceApiRestTemplate> target(){
    return IcecreamServiceApiRestTemplate.class;
  }

  interface IcecreamServiceApiRestTemplate extends IcecreamServiceApi{}

  //to not detect blocking calls
  @Override
  protected Scheduler testScheduler(){
    return Schedulers.boundedElastic();
  }
}
