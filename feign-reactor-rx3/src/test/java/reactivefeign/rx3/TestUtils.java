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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.TestObserver;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Helper methods for tests.
 */
class TestUtils {
  static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new JavaTimeModule());
  }

  public static <T> Predicate<T> equalsComparingFieldByFieldRecursively(T rhs) {
    return lhs -> {
      try {
        return MAPPER.writeValueAsString(lhs).equals(MAPPER.writeValueAsString(rhs));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T> io.reactivex.rxjava3.functions.Predicate<T> equalsComparingFieldByFieldRecursivelyRx(T rhs) {
    return lhs -> {
      try {
        return MAPPER.writeValueAsString(lhs).equals(MAPPER.writeValueAsString(rhs));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T> void assertValue(TestObserver<T> testObserver, @NonNull io.reactivex.rxjava3.functions.Predicate<T> valuePredicate) {
    assertTrue(testObserver.hasSubscription());
    testObserver.assertValue(valuePredicate)
            .assertNoErrors()
            .assertComplete();
  }

  public static <T> void assertNoValues(TestObserver<T> testObserver) {
    assertTrue(testObserver.hasSubscription());
    testObserver.assertNoValues()
            .assertNoErrors()
            .assertComplete();
  }

  public static <T> void assertError(TestObserver<T> testObserver, @NonNull Class<? extends Throwable> errorClass) {
    assertTrue(testObserver.hasSubscription());
    testObserver.assertError(errorClass);
  }
}
