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
package reactivefeign.rx3;

import org.junit.jupiter.api.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.rx3.testcase.IcecreamServiceApi;
import reactivefeign.rx3.testcase.IcecreamServiceApiBroken;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sergii Karpenko
 */

public class ContractTest {

  protected <T> ReactiveFeign.Builder<T> builder(){
    return Rx3ReactiveFeign.builder();
  }

  @Test
  public void shouldFailOnBrokenContract() {

    Throwable exception = assertThrows(IllegalArgumentException.class, () ->

      this.<IcecreamServiceApi>builder()
              .contract(targetType -> {
                throw new IllegalArgumentException("Broken Contract");
              })
              .target(IcecreamServiceApi.class, "http://localhost:8888"));
    assertThat(exception.getMessage(), containsString("Broken Contract"));
  }

  @Test
  public void shouldFailIfNotReactiveContract() {

    Throwable exception = assertThrows(IllegalArgumentException.class, () ->

      this.<IcecreamServiceApiBroken>builder()
              .target(IcecreamServiceApiBroken.class, "http://localhost:8888"));
    assertThat(exception.getMessage(), containsString("IcecreamServiceApiBroken#findOrder(int)"));
  }

}
