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
package reactivefeign;

import feign.InvocationHandlerFactory;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * {@link InvocationHandler} implementation that transforms calls to methods of feign contract into
 * asynchronous HTTP requests via spring WebClient.
 *
 * @author Sergii Karpenko
 */
public final class ReactiveInvocationHandler implements InvocationHandler {
  private final Target<?> target;
  private final Map<Method, MethodHandler> dispatch;

  private ReactiveInvocationHandler(final Target<?> target,
      final Map<Method, MethodHandler> dispatch) {
    this.target = checkNotNull(target, "target must not be null");
    this.dispatch = checkNotNull(dispatch, "dispatch must not be null");
    defineObjectMethodsHandlers();
  }

  private void defineObjectMethodsHandlers() {
    try {
      dispatch.put(Object.class.getMethod("equals", Object.class),
          args -> {
            Object otherHandler = args.length > 0 && args[0] != null
                ? Proxy.getInvocationHandler(args[0])
                : null;
            return equals(otherHandler);
          });
      dispatch.put(Object.class.getMethod("hashCode"),
          args -> hashCode());
      dispatch.put(Object.class.getMethod("toString"),
          args -> toString());
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    return dispatch.get(method).invoke(args);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof ReactiveInvocationHandler) {
      final ReactiveInvocationHandler otherHandler = (ReactiveInvocationHandler) other;
      return this.target.equals(otherHandler.target);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return target.hashCode();
  }

  @Override
  public String toString() {
    return target.toString();
  }

  /**
   * Factory for ReactiveInvocationHandler.
   */
  public static final class Factory implements InvocationHandlerFactory {

    @Override
    public InvocationHandler create(final Target target,
                                    final Map<Method, MethodHandler> dispatch) {
      return new ReactiveInvocationHandler(target, dispatch);
    }
  }
}
