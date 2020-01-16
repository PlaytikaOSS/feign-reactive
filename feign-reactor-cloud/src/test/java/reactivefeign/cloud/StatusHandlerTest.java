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
import feign.RetryableException;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.testcase.IcecreamServiceApi;

import java.util.function.Predicate;

/**
 * @author Sergii Karpenko
 */
public class StatusHandlerTest extends reactivefeign.StatusHandlerTest {

  @Override
  protected ReactiveFeignBuilder<IcecreamServiceApi> builder() {
    return BuilderUtils.cloudBuilderWithExecutionTimeoutDisabled();
  }

  @Override
  protected Predicate<Throwable> customException(){
    return throwable -> throwable instanceof HystrixRuntimeException
            && throwable.getCause() instanceof UnsupportedOperationException;
  }

  @Override
  protected Predicate<Throwable> feignDefaultException(){
    return throwable -> throwable instanceof HystrixRuntimeException
            && throwable.getCause() instanceof FeignException;
  }

  @Override
  protected Predicate<Throwable> customException1(){
    return throwable -> throwable instanceof HystrixRuntimeException
            && throwable.getCause() instanceof RetryableException;
  }

  @Override
  protected Predicate<Throwable> customException2(){
    return throwable -> throwable instanceof HystrixRuntimeException
            && throwable.getCause() instanceof RuntimeException;
  }
}
