
[![CircleCI](https://circleci.com/gh/Playtika/feign-reactive/tree/develop.svg?style=shield&circle-token=7436cccc44c3229204d0d94c3a1606feb02cb534)](https://circleci.com/gh/Playtika/feign-reactive/tree/develop)
[![codecov](https://codecov.io/gh/Playtika/feign-reactive/branch/develop/graph/badge.svg)](https://codecov.io/gh/Playtika/feign-reactive)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ce80f97d24fb4371a9f71cf44e94b0b0)](https://www.codacy.com/app/PlaytikaGithub/feign-reactive?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Playtika/feign-reactive&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.playtika.reactivefeign/feign-reactor/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.playtika.reactivefeign/feign-reactor)


# feign-reactive

Use Feign with Spring WebFlux

## Overview

Implementation of Feign on Spring WebClient. Brings you the best of two worlds together : 
concise syntax of Feign to write client side API on fast, asynchronous and
non-blocking HTTP client of Spring WebClient.

## Modules
  
  **_feign-reactor-core_** : base classes and interfaces that should allow to implement alternative reactor Feign
  
  **_feign-reactor-webclient_** : Spring WebClient based implementation of reactor Feign 
  
  **_feign-reactor-cloud_** : Spring Cloud implementation of reactor Feign (Ribbon/Hystrix)
  
  **_feign-reactor-java11_** : Java 11 HttpClient based implementation of reactor Feign (!! Winner of benchmarks !!)
  
  **_feign-reactor-rx2_** : Rx2 compatible implementation of reactor Feign (depends on feign-reactor-webclient)
  
  **_feign-reactor-jetty_** : experimental Reactive Jetty client based implementation of reactor Feign (doesn't depend on feign-reactor-webclient). In future will allow to write pure Rx2 version.
  - have greater reactivity level then Spring WebClient. By default don't collect body to list instead starts sending request body as stream. 
  - starts receiving reactive response before all reactive request body has been sent
  - process Flux<`String`> correctly in request and response body  

## Usage

Write Feign API as usual, but every method of interface
 - may accept `org.reactivestreams.Publisher` as body
 - must return `reactor.core.publisher.Mono` or `reactor.core.publisher.Flux`.

```java
@Headers({ "Accept: application/json" })
public interface IcecreamServiceApi {

  @RequestLine("GET /icecream/flavors")
  Flux<Flavor> getAvailableFlavors();

  @RequestLine("GET /icecream/mixins")
  Flux<Mixin> getAvailableMixins();

  @RequestLine("POST /icecream/orders")
  @Headers("Content-Type: application/json")
  Mono<Bill> makeOrder(IceCreamOrder order);

  @RequestLine("GET /icecream/orders/{orderId}")
  Mono<IceCreamOrder> findOrder(@Param("orderId") int orderId);

  @RequestLine("POST /icecream/bills/pay")
  @Headers("Content-Type: application/json")
  Mono<Void> payBill(Publisher<Bill> bill);
}
```
Build the client :

```java

/* Create instance of your API */
IcecreamServiceApi client = 
             WebReactiveFeign  //WebClient based reactive feign  
             //JettyReactiveFeign //Jetty http client based
             //Java11ReactiveFeign //Java 11 http client based
            .<IcecreamServiceApi>builder()
            .target(IcecreamServiceApi.class, "http://www.icecreame.com")

/* Execute nonblocking requests */
Flux<Flavor> flavors = icecreamApi.getAvailableFlavors();
Flux<Mixin> mixins = icecreamApi.getAvailableMixins();
```

or cloud aware client :

```java
 IcecreamServiceApi client = CloudReactiveFeign.<IcecreamServiceApi>builder(WebReactiveFeign.builder())
            .setLoadBalancerCommandFactory(s -> LoadBalancerCommand.builder()
                    .withLoadBalancer(AbstractLoadBalancer.class.cast(getNamedLoadBalancer(serviceName)))
                    .withRetryHandler(new DefaultLoadBalancerRetryHandler(1, 1, true))
                    .build())
            .fallback(() -> Mono.just(new IcecreamServiceApi() {
                @Override
                public Mono<String> get() {
                    return Mono.just("fallback");
                }
            }))
            .target(IcecreamServiceApi.class,  "http://" + serviceName);

/* Execute nonblocking requests */
Flux<Flavor> flavors = icecreamApi.getAvailableFlavors();
Flux<Mixin> mixins = icecreamApi.getAvailableMixins();
```

## Rx2 Usage 

Write Feign API as usual, but every method of interface
 - may accept `Flowable`, `Observable`, `Single` or `Maybe` as body
 - must return `Flowable`, `Observable`, `Single` or `Maybe`.

```java
@Headers({"Accept: application/json"})
public interface IcecreamServiceApi {

  @RequestLine("GET /icecream/flavors")
  Flowable<Flavor> getAvailableFlavors();

  @RequestLine("GET /icecream/mixins")
  Observable<Mixin> getAvailableMixins();

  @RequestLine("POST /icecream/orders")
  @Headers("Content-Type: application/json")
  Single<Bill> makeOrder(IceCreamOrder order);

  @RequestLine("GET /icecream/orders/{orderId}")
  Maybe<IceCreamOrder> findOrder(@Param("orderId") int orderId);

  @RequestLine("POST /icecream/bills/pay")
  @Headers("Content-Type: application/json")
  Single<Long> payBill(Bill bill);
```
Build the client :

```java

/* Create instance of your API */
IcecreamServiceApi client = Rx2ReactiveFeign
    .builder()
    .target(IcecreamServiceApi.class, "http://www.icecreame.com")

/* Execute nonblocking requests */
Flowable<Flavor> flavors = icecreamApi.getAvailableFlavors();
Observable<Mixin> mixins = icecreamApi.getAvailableMixins();
```

## Spring Auto-Configuration

You can enable auto-configuration of reactive Feign clients as Sring beans just by adding `feign-reactor-spring-configuration` module to classpath. 
[Spring Auto-Configuration module](https://github.com/Playtika/feign-reactive/tree/develop/feign-reactor-spring-configuration)
[Sample cloud auto-configuration project with Eureka/WebFlux/ReaciveFeign](https://github.com/kptfh/feign-reactive-sample)

## License

Library distributed under Apache License Version 2.0.
