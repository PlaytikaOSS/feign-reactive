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
In cloudless mode you need to specify `url` attribute.

To configure you feign client as cloud ready (Hystrix + Ribbon) you need to add 
`feign-reactor-cloud` module to your classpath.

This may be useful in case of tests:
- set `reactive.feign.cloud.enabled` to `false` to disable cloud configuration for all clients
- set `reactive.feign.ribbon.enabled` to `false` to disable ribbon configuration for all clients 
- set `reactive.feign.hystrix.enabled` to `false` to disable hystrix configuration for all clients 

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
- `requestInterceptors` : classes of `ReactiveHttpRequestInterceptor`. May be used to setup specific headers to request
- `logger` : class of `ReactiveLoggerListener`
- `decode404` : the same as in regular feign

## Configuration via configuration class
`@EnableReactiveFeignClients` annotation has `defaultConfiguration` attribute to specify default configuration class that will be used for all clients.

`@ReactiveFeignClient` annotation has `configuration` attribute for client specific configuration.

Here is the list of bean classes that will be used by reactive feign client if they declared in configuration class:
`ReactiveOptions.Builder`, `ReactiveRetryPolicy`, `List<Class<ReactiveHttpRequestInterceptor>>`, `ReactiveStatusHandler`,
`feign.codec.ErrorDecoder`, `ReactiveLoggerListener`

## Cloud specific configuration
#### Ribbon
`LoadBalancerCommandFactory` or `RetryHandler` beans defined in configuration will used to configure ribbon for corresponding client
####Hystrix
`CloudReactiveFeign.SetterFactory` bean defined in configuration will be used to configure hystrix for client

## Not all the same
- Use `ReactiveRetryPolicy` as part of Reactive Feign configuration if you need to specify backoff period between retries.
  You can't anymore use Spring's `RetryPolicy` as part of Ribbon configuration if you need to configure backoff.

- Use `ReactiveOptions.Builder` as part of Reactive Feign configuration if you need to specify connect and read timeout.
  Each reactive http client has it's own set of properties that describes request timeouts.
  Ribbon request timeouts properties are ignored. 
  
- If you need to override default hystrix timeout the only way to do it is to specify it 
in `CloudReactiveFeign.SetterFactory` bean in client configuration. `HystrixObservableCommand` has `SEMAPHORE` as default isolation strategy.
And Spring does not provide ability to configure execution timeout for this strategy via properties.  
     