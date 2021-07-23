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
package reactivefeign.utils;

import feign.MethodMetadata;
import feign.Request;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static feign.Feign.configKey;
import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;

public class FeignUtils {

  public static Class returnPublisherType(MethodMetadata methodMetadata) {
    return returnPublisherType(methodMetadata.returnType());
  }

  public static boolean isResponsePublisher(Type returnType) {
    Class<?> publisher = (Class)((ParameterizedType) returnType).getRawType();
    Type typeInPublisher = resolveLastTypeParameter(returnType, publisher);
    return isResponsePublisher(publisher, typeInPublisher);
  }

  public static Class returnPublisherType(Type returnType) {
    Class<?> publisher = (Class)((ParameterizedType) returnType).getRawType();
    Type typeInPublisher = resolveLastTypeParameter(returnType, publisher);
    if(isResponsePublisher(publisher, typeInPublisher)){
      Type typeInResponse = resolveLastTypeParameter(typeInPublisher, ReactiveHttpResponse.class);
      return (Class)((ParameterizedType) typeInResponse).getRawType();
    } else {
      return publisher;
    }
  }

  private static boolean isResponsePublisher(Class<?> publisher, Type typeInPublisher ){
    return publisher == Mono.class
            && typeInPublisher instanceof ParameterizedType
            && ((ParameterizedType) typeInPublisher).getRawType() == ReactiveHttpResponse.class;
  }

  public static Type returnActualType(MethodMetadata methodMetadata) {
    Type returnType = methodMetadata.returnType();
    return returnActualType(returnType);
  }

  public static Type returnActualType(Type returnType) {
    Class<?> publisher = (Class)((ParameterizedType) returnType).getRawType();
    Type typeInPublisher = resolveLastTypeParameter(returnType, publisher);
    if(isResponsePublisher(publisher, typeInPublisher)){
      Type typeInResponse = resolveLastTypeParameter(typeInPublisher, ReactiveHttpResponse.class);
      Class publisherInResponse = (Class)((ParameterizedType) typeInResponse).getRawType();
      return resolveLastTypeParameter(typeInResponse, publisherInResponse);
    } else {
      return typeInPublisher;
    }
  }

  public static Type bodyActualType(MethodMetadata methodMetadata) {
    return getBodyActualType(methodMetadata.bodyType());
  }

  public static Type getBodyActualType(Type bodyType) {
    return ofNullable(bodyType).map(type -> {
      if (type instanceof ParameterizedType) {
        Class<?> bodyClass = (Class<?>) ((ParameterizedType) type).getRawType();
        if (Publisher.class.isAssignableFrom(bodyClass)) {
          return resolveLastTypeParameter(bodyType, bodyClass);
        }
        else {
          return type;
        }
      }
      else {
        return type;
      }
    }).orElse(null);
  }

  public static Method findMethodInTarget(Target target, MethodMetadata methodMetadata) {
    return Arrays.stream(target.type().getMethods())
            .filter(method -> configKey(target.type(), method).equals(methodMetadata.configKey()))
            .findFirst().orElseThrow(IllegalArgumentException::new);
  }

  public static boolean requestWithBody(MethodMetadata methodMetadata){
    return methodMetadata.bodyType() != null;
  }

  public static boolean responseWithBody(MethodMetadata methodMetadata){
    return returnActualType(methodMetadata) != Void.class;
  }

  public static Request.HttpMethod httpMethod(String method){
    return Request.HttpMethod.valueOf(method.toUpperCase());
  }
}
