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
package reactivefeign.client.log;

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.client.DelegatingReactiveHttpResponse;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpExchangeFilterFunction;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.utils.SerializedFormData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static reactivefeign.utils.FeignUtils.requestWithBody;
import static reactivefeign.utils.FeignUtils.responseWithBody;

/**
 * Wraps {@link ReactiveHttpClient} with log logic
 * May be used to collect request execution metrics
 *
 * @author Sergii Karpenko
 */
public class LoggerExchangeFilterFunction<P extends Publisher<?>> implements ReactiveHttpExchangeFilterFunction<P> {

  private final MethodMetadata methodMetadata;
  private Target target;
  private final ReactiveLoggerListener<Object> loggerListener;
  private final boolean requestWithBody;
  private final boolean responseWithBody;

  public static <P extends Publisher<?>> ReactiveHttpExchangeFilterFunction<P> log(
          MethodMetadata methodMetadata, Target target,
          ReactiveLoggerListener<Object> loggerListener) {
    return new LoggerExchangeFilterFunction<>(methodMetadata, target, loggerListener);
  }

  private LoggerExchangeFilterFunction(MethodMetadata methodMetadata, Target target,
                                       ReactiveLoggerListener<Object> loggerListener) {
    this.methodMetadata = methodMetadata;
    this.target = target;
    this.loggerListener = loggerListener;
    requestWithBody = requestWithBody(methodMetadata);
    responseWithBody = responseWithBody(methodMetadata);
  }

  @Override
  public Mono<ReactiveHttpResponse<P>> filter(ReactiveHttpRequest request, ReactiveHttpClient<P> exchangeFunction) {
    AtomicReference<Object> logContext = new AtomicReference<>();

    logContext.set(loggerListener.requestStarted(request, target, methodMetadata));

    if(loggerListener.logRequestBody()){
      request = logRequestBody(request, logContext.get());
    }

    return exchangeFunction.executeRequest(request)
            .doOnNext(resp -> loggerListener.responseReceived(resp, logContext.get()))
            .doOnError(throwable -> loggerListener.errorReceived(throwable, logContext.get()))
            .map(resp -> {
              if(loggerListener.logResponseBody()){
                return logResponseBody(resp, logContext.get());
              } else {
                return resp;
              }
            });
  }

  private ReactiveHttpRequest logRequestBody(ReactiveHttpRequest request, Object logContext) {

    if(requestWithBody) {
      Publisher<Object> bodyLogged;
      if (request.body() instanceof Mono) {
        bodyLogged = ((Mono<Object>) request.body())
                .doOnNext(requestBodyLogger(logContext));
      } else if (request.body() instanceof Flux) {
        bodyLogged = ((Flux<Object>) request.body())
                .doOnNext(requestBodyLogger(logContext));
      } else if(request.body() instanceof SerializedFormData){
        bodyLogged =  ((SerializedFormData) request.body()).logged(requestBodyLogger(logContext));
      } else {
        throw new IllegalArgumentException("Unsupported publisher type: " + request.body().getClass());
      }
      return new ReactiveHttpRequest(request, bodyLogged);
    }

    return request;
  }

  private Consumer<Object> requestBodyLogger(Object logContext) {
    return body -> loggerListener.bodySent(body, logContext);
  }

  private ReactiveHttpResponse<P> logResponseBody(ReactiveHttpResponse<P> resp, Object logContext) {
    return responseWithBody ? new LoggerReactiveHttpResponse<>(resp, loggerListener, logContext) : resp;
  }

  private static class LoggerReactiveHttpResponse<P extends Publisher<?>> extends DelegatingReactiveHttpResponse<P> {

    private final ReactiveLoggerListener<Object> loggerListener;
    private Object logContext;

    private LoggerReactiveHttpResponse(ReactiveHttpResponse<P> response,
                                       ReactiveLoggerListener<Object> loggerListener, Object logContext) {
      super(response);
      this.loggerListener = loggerListener;
      this.logContext = logContext;
    }

    @Override
    public P body() {
      P publisher = getResponse().body();

      if (publisher instanceof Mono) {
        return (P)((Mono) publisher).doOnNext(responseBodyLogger());
      } else {
        return (P)((Flux) publisher).doOnNext(responseBodyLogger());
      }

    }

    @Override
    public Mono<byte[]> bodyData() {
      Mono<byte[]> publisher = getResponse().bodyData();

      return publisher.doOnNext(responseBodyLogger());
    }

    private Consumer<Object> responseBodyLogger() {
      return result -> loggerListener.bodyReceived(result, logContext);
    }
  }

}
