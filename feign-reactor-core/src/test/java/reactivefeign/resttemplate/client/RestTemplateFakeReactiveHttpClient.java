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
package reactivefeign.resttemplate.client;

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.utils.SerializedFormData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.springframework.core.ParameterizedTypeReference.forType;
import static reactivefeign.utils.FeignUtils.returnActualType;
import static reactivefeign.utils.FeignUtils.returnPublisherType;

public class RestTemplateFakeReactiveHttpClient implements ReactiveHttpClient {

  private final RestTemplate restTemplate;
  private final boolean acceptGzip;
  private final Type returnPublisherType;
  private final ParameterizedTypeReference<Object> returnActualType;

  RestTemplateFakeReactiveHttpClient(MethodMetadata methodMetadata,
                                     RestTemplate restTemplate,
                                     boolean acceptGzip) {
    this.restTemplate = restTemplate;
    this.acceptGzip = acceptGzip;

    returnPublisherType = returnPublisherType(methodMetadata);
    returnActualType = forType(returnActualType(methodMetadata));
  }

  @Override
  public Mono<ReactiveHttpResponse> executeRequest(ReactiveHttpRequest request) {

    Mono<Object> bodyMono;
    if(request.body() instanceof SerializedFormData){
      bodyMono = Mono.just(((SerializedFormData) request.body()).getFormData());
    }
    else if (request.body() instanceof Mono) {
      bodyMono = ((Mono<Object>) request.body());
    } else if (request.body() instanceof Flux) {
      bodyMono = ((Flux) request.body()).collectList();
    } else {
      bodyMono = Mono.just(request.body());
    }
    Mono<Object> bodyMonoFinal = bodyMono.switchIfEmpty(Mono.just(new byte[0]));

    return Mono.defer(() -> bodyMonoFinal).<ReactiveHttpResponse>flatMap(body -> {
              MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(request.headers());
              if (acceptGzip) {
                headers.add("Accept-Encoding", "gzip");
              }

              return Mono.fromCallable(() -> {
                ResponseEntity response = restTemplate.exchange(
                        request.uri(), HttpMethod.valueOf(request.method()),
                        new HttpEntity<>(body, headers), responseType());
                return new FakeReactiveHttpResponse(request, response, returnPublisherType);
              });
            })
            .onErrorResume(HttpStatusCodeException.class,
                    ex -> Mono.just(new ErrorReactiveHttpResponse(request, ex)))
            .onErrorMap(ex -> {
                      if (ex instanceof ResourceAccessException
                              && ex.getCause() instanceof SocketTimeoutException) {
                        return new ReadTimeoutException(ex.getCause(), request);
                      } else {
                        return new ReactiveFeignException(ex, request);
                      }
                    }
            );
  }

  private ParameterizedTypeReference<Object> responseType(){
    if (returnPublisherType == Mono.class) {
      return returnActualType;
    } else {
      return forType(new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return new Type[] {returnActualType.getType()};
        }

        @Override
        public Type getRawType() {
          return List.class;
        }

        @Override
        public Type getOwnerType() {
          return null;
        }
      });
    }
  }

  private static class FakeReactiveHttpResponse implements ReactiveHttpResponse{

    private ReactiveHttpRequest request;
    private final ResponseEntity response;
    private final Type returnPublisherType;

    private FakeReactiveHttpResponse(ReactiveHttpRequest request, ResponseEntity response, Type returnPublisherType) {
      this.request = request;
      this.response = response;
      this.returnPublisherType = returnPublisherType;
    }

    @Override
    public ReactiveHttpRequest request() {
      return request;
    }

    @Override
    public int status() {
      return response.getStatusCodeValue();
    }

    @Override
    public Map<String, List<String>> headers() {
      return response.getHeaders();
    }

    @Override
    public Publisher<Object> body() {
      if (returnPublisherType == Mono.class) {
        return Mono.justOrEmpty(response.getBody());
      } else {
        return Flux.fromIterable((List)response.getBody());
      }
    }

    @Override
    public Mono<Void> releaseBody() {
      return Mono.empty();
    }

    @Override
    public Mono<byte[]> bodyData() {
      return Mono.just(new byte[0]);
    }
  }

  private static class ErrorReactiveHttpResponse implements ReactiveHttpResponse{

    private ReactiveHttpRequest request;
    private final HttpStatusCodeException ex;

    private ErrorReactiveHttpResponse(ReactiveHttpRequest request, HttpStatusCodeException ex) {
      this.request = request;
      this.ex = ex;
    }

    @Override
    public ReactiveHttpRequest request() {
      return request;
    }

    @Override
    public int status() {
      return ex.getStatusCode().value();
    }

    @Override
    public Map<String, List<String>> headers() {
      return ex.getResponseHeaders();
    }

    @Override
    public Publisher<Object> body() {
      return Mono.empty();
    }

    @Override
    public Mono<Void> releaseBody() {
      return Mono.empty();
    }

    @Override
    public Mono<byte[]> bodyData() {
      return Mono.just(ex.getResponseBodyAsByteArray());
    }
  }
}
