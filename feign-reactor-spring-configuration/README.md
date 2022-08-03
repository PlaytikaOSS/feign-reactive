#### Intro
Reactive Feign client Spring auto-configuration is patterned after regular Feign client auto-configuration.
If you already familiar with blocking/sync Feign client you will find that both auto-configurations has may things in common.
In any case if this documentation looks very brief you can check first Spring documentation regarding auto-conf of regular Feign client.   

# Spring Auto Configuration

To enable auto-configuration of reactive feign clients you need to add `@EnableReactiveFeignClients` annotation to 
your Spring Boot configuration class.
Thus all interfaces annotated with `@ReactiveFeignClient` annotation will be instantiated and configured as Spring beans

You should specify `name` attribute in this annotation. Feign client bean will be registered with the name of `name + "ReactiveFeignClient"`
if you had not specified `qualifier` attribute.

In cloud mode `name` attribute will be used as eureka application name. 
If you need several feign clients for one eureka application 
you should specify eureka application manually in application.yml for feign client with name 
that differs from eureka application name.
See example in [sample project](https://github.com/kptfh/feign-reactive-sample/blob/master/feign/src/main/resources/application.yml)

In cloudless mode you need to specify `url` attribute.

# Cloud (Hystrix + Ribbon)

To configure you feign client as cloud ready (Hystrix + Ribbon) you need to add 
`feign-reactor-cloud` module to your classpath.

This may be useful in case of tests:
- set `reactive.feign.cloud.enabled` to `false` to disable cloud configuration for all clients
- set `reactive.feign.ribbon.enabled` to `false` to disable loadbalancer configuration for all clients 
- set `reactive.feign.hystrix.enabled` to `false` to disable hystrix configuration for all clients 
- set `reactive.feign.logger.enabled` to `true` to enable default logger
- set `reactive.feign.metrics.enabled` to `true` to enable default Micrometer logger. 
  Also make sure that you added actual Micrometer implementation module into your project dependency 

There are to ways to configure specific settings for feign clients: 
via `application.properties` file and configuration class. In both approaches it's possible specify default settings 
and override them for specific clients.

## application.properties configuration
Each property should be prefixed with `reactive.feign.client.config.<client-name>`

All reactive feign client specific properties Spring read by  ``ReactiveFeignClientProperties`` class.

Here is the list of available properties:
- `options` : reactive http client specific options. Check implementation of `ReactiveOptions.Builder` for each client.
- `retry` : retry configuration (check `ReactiveFeignClientProperties.RetryConfiguration` for details)      
- `statusHandler` : class that implements `ReactiveStatusHandler`. Is a replacement of regular feign `ErrorDecoder`.  
- `errorMapper` : class that implements `ReactiveErrorMapper`. 
- `requestInterceptors` : classes of `ReactiveHttpRequestInterceptor`. May be used to setup specific headers to request
- `logger` : class of `ReactiveLoggerListener`
- `metricsLogger` : class of `MicrometerReactiveLogger`
- `decode404` : the same as in regular feign

## Configuration via configuration class
`@EnableReactiveFeignClients` annotation has `defaultConfiguration` attribute to specify default configuration class that will be used for all clients.

`@ReactiveFeignClient` annotation has `configuration` attribute for client specific configuration.

Here is the list of bean classes that will be used by reactive feign client if they declared in configuration class:
`ReactiveOptions.Builder`, `ReactiveRetryPolicies`, `List<Class<ReactiveHttpRequestInterceptor>>`, `ReactiveStatusHandler`,
`feign.codec.ErrorDecoder`, `ReactiveErrorMapper`, `ReactiveLoggerListener`, `MicrometerReactiveLogger`

## Cloud specific configuration

#### Ribbon
`ReactiveLoadBalancer.Factory` or `ReactiveRetryPolicies` beans defined in configuration will used to configure loadbalancer for corresponding client

#### Hystrix
`CloudReactiveFeign.SetterFactory` bean defined in configuration will be used to configure hystrix for client

## Not all the same
#### Retry with backoff
Use `ReactiveRetryPolicy` or `ReactiveRetryPolicy.Builder` as part of Reactive Feign configuration 
  if you need to specify backoff period between retries.
  You can't anymore use Spring's `RetryPolicy` as part of Ribbon configuration if you need to configure backoff.
```
reactive.feign.client.config.<client-name>.retry.builder=reactivefeign.retry.BasicReactiveRetryPolicy.Builder
reactive.feign.client.config.<client-name>.retry.args.maxRetries=2
reactive.feign.client.config.<client-name>.retry.args.backoffInMs=10
```
#### Configure timeouts
Use `ReactiveOptions.Builder` as part of Reactive Feign configuration if you need to specify connect and read timeout.
  Each reactive http client has it's own set of properties that describes request timeouts.
  Ribbon request timeouts properties are ignored. 
     
# Cloud2 (CircuitBreaker + LoadBalancer)

In Spring Cloud  2.2.0 more generic abstraction vere introduced for cloud environment. 
`CircuitBreaker` is the replacement of `Hystrix` (now is one of implementations)
`LoadBalancer` is use instead of `Ribbon`

So below are the differences with old `cloud` module.

To configure you feign client as cloud ready (CircuitBreaker + LoadBalancer) you need to add 
`feign-reactor-cloud` module to your classpath and exclude `feign-reactor-cloud`.

This may be useful in case of tests:
- set `reactive.feign.loadbalancer.enabled` to `false` to disable loadbalancer configuration for all clients 
- set `reactive.feign.circuit.breaker.enabled` to `false` to disable hystrix configuration for all clients 

## application.properties configuration

As for now `LoadBalance` doesnt have it's retry configuration so here is the list of new properties:
- `retryOnSame` : configure number of retries for the same server (check `ReactiveFeignClientProperties.RetryConfiguration` for details) 
- `retryOnNext` : configure number of retries for the next server (check `ReactiveFeignClientProperties.RetryConfiguration` for details)

## Configuration via configuration class

To configure `LoadBalancer` define your own `ReactiveLoadBalancer.Factory` bean in feign client configuration.

To configure `CircuitBreaker` you may use `ReactiveFeignCircuitBreakerFactory` bean 
or `ReactiveCircuitBreakerFactory` with `ReactiveFeignCircuitBreakerCustomizer` bean defined in feign client configuration.

Note: It's highly recommended to switch to `cloud2` module as we'll stop support of `cloud` module in some time.
There is an option to switch to `cloud2` with well known Ribbon and Hystrix implementations.   
     