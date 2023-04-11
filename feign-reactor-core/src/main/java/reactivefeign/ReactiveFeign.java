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

import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveErrorMapper;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpClientFactory;
import reactivefeign.client.ReactiveHttpExchangeFilterFunction;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.ReactiveHttpResponseMapper;
import reactivefeign.client.ResponseMappers;
import reactivefeign.client.log.DefaultReactiveLogger;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.client.statushandler.ReactiveStatusHandlers;
import reactivefeign.methodhandler.DefaultMethodHandler;
import reactivefeign.methodhandler.MethodHandler;
import reactivefeign.methodhandler.MethodHandlerFactory;
import reactivefeign.methodhandler.ReactiveMethodHandlerFactory;
import reactivefeign.methodhandler.fallback.FallbackMethodHandlerFactory;
import reactivefeign.publisher.FluxPublisherHttpClient;
import reactivefeign.publisher.MonoPublisherHttpClient;
import reactivefeign.publisher.PublisherClientFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.publisher.ResponsePublisherHttpClient;
import reactivefeign.publisher.retry.FluxRetryPublisherHttpClient;
import reactivefeign.publisher.retry.MonoRetryPublisherHttpClient;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static feign.Util.checkNotNull;
import static feign.Util.isDefault;
import static reactivefeign.client.ReactiveHttpExchangeFilterFunction.ofErrorMapper;
import static reactivefeign.client.ReactiveHttpExchangeFilterFunction.ofRequestProcessor;
import static reactivefeign.client.ReactiveHttpExchangeFilterFunction.ofResponseProcessor;
import static reactivefeign.client.StatusHandlerPostProcessor.handleStatus;
import static reactivefeign.client.log.LoggerExchangeFilterFunction.log;
import static reactivefeign.utils.FeignUtils.isResponsePublisher;
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
    Map<String, MethodMetadata> metadata = contract.parseAndValidateMetadata(target.type())
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
    protected List<ReactiveHttpExchangeFilterFunction> exchangeFilterFunctions = new ArrayList<>();
    protected ReactiveStatusHandler statusHandler = ReactiveStatusHandlers.defaultFeignErrorDecoder();
    protected ReactiveErrorMapper errorMapper;
    protected List<ReactiveLoggerListener<Object>> loggerListeners = new ArrayList<>();
    protected InvocationHandlerFactory invocationHandlerFactory =
            new ReactiveInvocationHandler.Factory();
    protected boolean decode404 = false;

    private ReactiveRetryPolicy retryPolicy;
    protected FallbackFactory fallbackFactory;

    protected Builder(){
      contract(new Contract.Default());
      addLoggerListener(new DefaultReactiveLogger(Clock.systemUTC()));
    }

    abstract protected ReactiveHttpClientFactory clientFactory();

    @Override
    public Builder<T> contract(final Contract contract) {
      this.contract = new ReactiveContract(contract);
      return this;
    }

    @Override
    public Builder<T> addRequestInterceptor(ReactiveHttpRequestInterceptor requestInterceptor) {
      this.exchangeFilterFunctions.add(ofRequestProcessor(requestInterceptor));
      return this;
    }

    @Override
    public Builder<T> addLoggerListener(ReactiveLoggerListener loggerListener) {
      this.loggerListeners.add(loggerListener);
      return this;
    }

    @Override
    public Builder<T> decode404() {
      this.decode404 = true;
      return this;
    }

    @Override
    public ReactiveFeignBuilder<T> addExchangeFilterFunction(ReactiveHttpExchangeFilterFunction exchangeFilterFunction){
      this.exchangeFilterFunctions.add(exchangeFilterFunction);
      return this;
    }

    @Override
    public Builder<T> errorMapper(ReactiveErrorMapper errorMapper){
      this.errorMapper = errorMapper;
      return this;
    }

    @Override
    public Builder<T> statusHandler(ReactiveStatusHandler statusHandler) {
      this.statusHandler = statusHandler;
      return this;
    }

    @Override
    public Builder<T> responseMapper(ReactiveHttpResponseMapper responseMapper) {
      this.exchangeFilterFunctions.add(ofResponseProcessor(responseMapper));
      return this;
    }

    @Override
    public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
      return this;
    }

    @Override
    public Builder<T> fallback(T fallback) {
      return fallbackFactory(throwable -> fallback);
    }

    @Override
    public Builder<T> fallbackFactory(FallbackFactory<T> fallbackFactory) {
      this.fallbackFactory = fallbackFactory;
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

      ReactiveHttpClientFactory clientFactory = clientFactory();

      return new PublisherClientFactory(){

        Target target;

        @Override
        public void target(Target target) {
          clientFactory.target(target);
          this.target = target;
        }

        @Override
        public PublisherHttpClient create(MethodMetadata methodMetadata) {
          checkNotNull(clientFactory,
                  "clientFactory wasn't provided in ReactiveFeign builder");

          ReactiveHttpClient reactiveClient = clientFactory.create(methodMetadata);

          List<ReactiveHttpExchangeFilterFunction> exchangeFilterFunctionsAll = new ArrayList(exchangeFilterFunctions);

          if(errorMapper != null){
            exchangeFilterFunctionsAll.add(ofErrorMapper(errorMapper));
          }

          if(statusHandler != null) {
            exchangeFilterFunctionsAll.add(ofResponseProcessor(handleStatus(statusHandler)));
          }

          if(decode404){
            exchangeFilterFunctionsAll.add(ofResponseProcessor(ResponseMappers.ignore404()));
          }

          for(ReactiveLoggerListener<Object> loggerListener : loggerListeners){
            exchangeFilterFunctionsAll.add(log(methodMetadata, target, loggerListener));
          }

          Optional<ReactiveHttpExchangeFilterFunction> exchangeFilterFunction = exchangeFilterFunctionsAll.stream()
                  .reduce(ReactiveHttpExchangeFilterFunction::then);
          if(exchangeFilterFunction.isPresent()){
            reactiveClient = exchangeFilterFunction.get().filter(reactiveClient);
          }

          reactivefeign.publisher.PublisherHttpClient publisherClient = toPublisher(reactiveClient, methodMetadata);
          if (retryPolicy != null) {
            publisherClient = retry(publisherClient, methodMetadata, retryPolicy);
          }

          return publisherClient;
        }
      };
    }

    @Override
    public MethodHandlerFactory buildReactiveMethodHandlerFactory(PublisherClientFactory reactiveClientFactory) {
      MethodHandlerFactory methodHandlerFactory = new ReactiveMethodHandlerFactory(reactiveClientFactory);
      return fallbackFactory != null
              ? new FallbackMethodHandlerFactory(methodHandlerFactory, (Function<Throwable, Object>) fallbackFactory)
              : methodHandlerFactory;
    }

    public static PublisherHttpClient retry(
            PublisherHttpClient publisherClient,
            MethodMetadata methodMetadata,
            ReactiveRetryPolicy retryPolicy) {
      Type returnPublisherType = returnPublisherType(methodMetadata);
      if(returnPublisherType == Mono.class){
        return new MonoRetryPublisherHttpClient(publisherClient, methodMetadata, retryPolicy);
      } else if(returnPublisherType == Flux.class) {
        return new FluxRetryPublisherHttpClient(publisherClient, methodMetadata, retryPolicy);
      } else {
        throw new IllegalArgumentException("Unknown returnPublisherType: " + returnPublisherType);
      }
    }

    protected PublisherHttpClient toPublisher(ReactiveHttpClient reactiveHttpClient, MethodMetadata methodMetadata){
      if(isResponsePublisher(methodMetadata.returnType())){
        return new ResponsePublisherHttpClient(reactiveHttpClient);
      }

      Class returnPublisherType = returnPublisherType(methodMetadata);
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
