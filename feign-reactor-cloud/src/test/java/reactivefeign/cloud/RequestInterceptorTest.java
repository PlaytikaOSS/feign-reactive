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
package reactivefeign.cloud;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import feign.FeignException;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.testcase.IcecreamServiceApi;

import java.util.function.Predicate;

import static reactivefeign.cloud.BuilderUtils.cloudBuilderWithExecutionTimeoutDisabled;

/**
 * @author Sergii Karpenko
 */
public class RequestInterceptorTest extends reactivefeign.RequestInterceptorTest {

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder() {
      return cloudBuilderWithExecutionTimeoutDisabled();
  }

  @Override
  protected Predicate<Throwable> notAuthorizedException() {
    return throwable -> throwable instanceof HystrixRuntimeException
            && throwable.getCause() instanceof FeignException;
  }
}
