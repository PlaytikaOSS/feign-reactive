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

import feign.*;
import feign.codec.ErrorDecoder;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactivefeign.methodhandler.DefaultMethodHandler;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.publisher.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static feign.Util.checkNotNull;
import static feign.Util.isDefault;
import static reactivefeign.client.InterceptorReactiveHttpClient.intercept;
import static reactivefeign.client.LoggerReactiveHttpClient.log;
import static reactivefeign.client.ReactiveHttpRequestInterceptors.composite;
import static reactivefeign.client.ResponseMappers.ignore404;
import static reactivefeign.client.ResponseMappers.mapResponse;
import static reactivefeign.client.StatusHandlerReactiveHttpClient.handleStatus;
import static reactivefeign.utils.FeignUtils.returnPublisherType;

/**
 * Allows Feign interfaces to accept {@link Publisher} as body and return reactive {@link Mono} or
 * {@link Flux}.
 *
 * @author Sergii Karpenko
 */
public class ReactiveFeign {

  private final Contract contract;
  private final MethodHandlerFactory methodHandlerFactory;
  private final InvocationHandlerFactory invocationHandlerFactory;

  protected ReactiveFeign(
          final Contract contract,
          final MethodHandlerFactory methodHandlerFactory,
          final InvocationHandlerFactory invocationHandlerFactory) {
    this.contract = contract;
    this.methodHandlerFactory = methodHandlerFactory;
    this.invocationHandlerFactory = invocationHandlerFactory;
  }

  @SuppressWarnings("unchecked")
  public <T> T newInstance(Target<T> target) {
    final Map<String, MethodHandler> nameToHandler = targetToHandlersByName(target);
    final Map<Method, InvocationHandlerFactory.MethodHandler> methodToHandler = new LinkedHashMap<>();
    final List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<>();

    for (final Method method : target.type().getMethods()) {
      if (isDefault(method)) {
        final DefaultMethodHandler handler = new DefaultMethodHandler(method);
        defaultMethodHandlers.add(handler);
        methodToHandler.put(method, handler);
      } else {
        methodToHandler.put(method,
                nameToHandler.get(Feign.configKey(target.type(), method)));
      }
    }

    final InvocationHandler handler = invocationHandlerFactory.create(target, methodToHandler);
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(),
            new Class<?>[] {target.type()}, handler);

    for (final DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
      defaultMethodHandler.bindTo(proxy);
    }

    return proxy;
  }

  Map<String, MethodHandler> targetToHandlersByName(final Target target) {
    Map<String, MethodMetadata> metadata = contract.parseAndValidatateMetadata(target.type())
            .stream()
            .collect(Collectors.toMap(
                    MethodMetadata::configKey,
                    md -> md
            ));
    Map<String, Method> configKeyToMethod = Stream.of(target.type().getMethods())
            .collect(Collectors.toMap(
                    method -> Feign.configKey(target.type(), method),
                    method -> method
            ));

    final Map<String, MethodHandler> result = new LinkedHashMap<>();

    methodHandlerFactory.target(target);

    for (final Map.Entry<String, Method> entry : configKeyToMethod.entrySet()) {
      String configKey = entry.getKey();
      MethodMetadata md = metadata.get(configKey);
      MethodHandler methodHandler = md != null
              ? methodHandlerFactory.create(md)
              : methodHandlerFactory.createDefault(entry.getValue());  //isDefault(entry.getValue())
      result.put(configKey, methodHandler);
    }

    return result;
  }

  /**
   * ReactiveFeign builder.
   */
  public abstract static class Builder<T> implements ReactiveFeignBuilder<T>{
    protected Contract contract;
    protected ReactiveHttpClientFactory clientFactory;
    protected List<ReactiveHttpRequestInterceptor> requestInterceptors = new ArrayList<>();
    protected BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> responseMapper;
    protected ReactiveStatusHandler statusHandler =
            ReactiveStatusHandlers.defaultFeign(new ErrorDecoder.Default());
    protected InvocationHandlerFactory invocationHandlerFactory =
            new ReactiveInvocationHandler.Factory();
    protected boolean decode404 = false;

    private ReactiveRetryPolicy retryPolicy;

    protected Builder(){
      contract(new Contract.Default());
    }

    protected Builder<T> clientFactory(ReactiveHttpClientFactory clientFactory) {
      this.clientFactory = clientFactory;
      return this;
    }

    @Override
    public Builder<T> contract(final Contract contract) {
      this.contract = new ReactiveContract(contract);
      return this;
    }

    @Override
    public Builder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
      this.requestInterceptors.add(requestInterceptor);
      return this;
    }

    @Override
    public Builder<T> decode404() {
      this.decode404 = true;
      return this;
    }

    @Override
    public Builder<T> statusHandler(ReactiveStatusHandler statusHandler) {
      this.statusHandler = statusHandler;
      return this;
    }

    @Override
    public Builder<T> responseMapper(BiFunction<MethodMetadata, ReactiveHttpResponse, ReactiveHttpResponse> responseMapper) {
      this.responseMapper = responseMapper;
      return this;
    }

    @Override
    public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
      return this;
    }

    @Override
    public Contract contract(){
      return contract;
    }

    @Override
    public InvocationHandlerFactory invocationHandlerFactory(){
      return invocationHandlerFactory;
    }

    @Override
    public PublisherClientFactory buildReactiveClientFactory() {
      return new PublisherClientFactory(){

        @Override
        public void target(Target target) {
          clientFactory.target(target);
        }

        @Override
        public PublisherHttpClient create(MethodMetadata methodMetadata) {
          checkNotNull(clientFactory,
                  "clientFactory wasn't provided in ReactiveFeign builder");

          ReactiveHttpClient reactiveClient = clientFactory.create(methodMetadata);

          if (!requestInterceptors.isEmpty()) {
            reactiveClient = intercept(reactiveClient, composite(requestInterceptors));
          }

          reactiveClient = log(reactiveClient, methodMetadata);

          if (responseMapper != null) {
            reactiveClient = mapResponse(reactiveClient, methodMetadata, responseMapper);
          }

          if (decode404) {
            reactiveClient = mapResponse(reactiveClient, methodMetadata, ignore404());
          }

          if (statusHandler != null) {
            reactiveClient = handleStatus(reactiveClient, methodMetadata, statusHandler);
          }

          reactivefeign.publisher.PublisherHttpClient publisherClient = toPublisher(reactiveClient, methodMetadata);
          if (retryPolicy != null) {
            publisherClient = retry(publisherClient, methodMetadata, retryPolicy.toRetryFunction());
          }

          return publisherClient;
        }
      };
    }

    protected PublisherHttpClient retry(
            PublisherHttpClient publisherClient,
            MethodMetadata methodMetadata,
            Function<Flux<Throwable>, Flux<Throwable>> retryFunction) {
      Type returnPublisherType = returnPublisherType(methodMetadata);
      if(returnPublisherType == Mono.class){
        return new MonoRetryPublisherHttpClient(
                (MonoPublisherHttpClient)publisherClient, methodMetadata, retryFunction);
      } else if(returnPublisherType == Flux.class) {
        return new FluxRetryPublisherHttpClient(
                (FluxPublisherHttpClient)publisherClient, methodMetadata, retryFunction);
      } else {
        throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
      }
    }

    protected PublisherHttpClient toPublisher(ReactiveHttpClient reactiveHttpClient, MethodMetadata methodMetadata){
      Type returnPublisherType = returnPublisherType(methodMetadata);
      if(returnPublisherType == Mono.class){
        return new MonoPublisherHttpClient(reactiveHttpClient);
      } else if(returnPublisherType == Flux.class){
        return new FluxPublisherHttpClient(reactiveHttpClient);
      } else {
        throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
      }
    }
  }

}
