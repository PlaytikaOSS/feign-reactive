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

import org.springframework.http.converter.HttpMessageNotReadableException;
import reactivefeign.ReactiveFeign;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.resttemplate.client.RestTemplateFakeReactiveFeign;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Predicate;

/**
 * @author Sergii Karpenko
 */
public class BasicFeaturesTest extends reactivefeign.BasicFeaturesTest {

  @Override
  protected <T> ReactiveFeign.Builder<T> builder() {
    return RestTemplateFakeReactiveFeign.builder();
  }

  @Override
  protected Predicate<Throwable> corruptedJsonError() {
    return throwable -> throwable instanceof ReactiveFeignException
            && throwable.getCause().getCause() instanceof HttpMessageNotReadableException;
  }

  //to not detect blocking calls
  @Override
  protected Scheduler testScheduler(){
    return Schedulers.boundedElastic();
  }
}
