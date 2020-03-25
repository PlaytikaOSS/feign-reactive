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
package reactivefeign.rx2;

import feign.Contract;
import feign.MethodMetadata;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static feign.Util.checkNotNull;
import static java.util.Arrays.asList;

/**
 * Contract allowing only {@link Mono} and {@link Flux} return type.
 *
 * @author Sergii Karpenko
 */
public class Rx2Contract implements Contract {

  public static final Set<Type> RX2_TYPES = new HashSet<>(asList(
          Flowable.class, Observable.class, Single.class, Maybe.class));

  private final Contract delegate;

  public Rx2Contract(final Contract delegate) {
    this.delegate = checkNotNull(delegate, "delegate must not be null");
  }

  @Override
  public List<MethodMetadata> parseAndValidateMetadata(final Class<?> targetType) {
    final List<MethodMetadata> methodsMetadata =
        this.delegate.parseAndValidateMetadata(targetType);

    for (final MethodMetadata metadata : methodsMetadata) {
      final Type type = metadata.returnType();
      if (!isRx2Type(type)) {
        throw new IllegalArgumentException(String.format(
            "Method %s of contract %s doesn't returns rx2 types",
            metadata.configKey(), targetType.getSimpleName()));
      }
    }

    return methodsMetadata;
  }

  private boolean isRx2Type(final Type type) {
    return (type instanceof ParameterizedType)
        && RX2_TYPES.contains(((ParameterizedType) type).getRawType());
  }


}
