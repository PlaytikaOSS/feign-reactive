/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.spring.config;

import feign.Contract;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactivefeign.*;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.statushandler.ReactiveStatusHandler;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;

/**
 *
 *  Patterned after org.springframework.cloud.openfeign.FeignClientFactoryBean
 */
class ReactiveFeignClientFactoryBean implements FactoryBean<Object>, InitializingBean,
		ApplicationContextAware {
	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
	 ***********************************/

	private Class<?> type;

	private String name;

	private String url;

	private String path;

	private boolean decode404;

	private ApplicationContext applicationContext;

	private Class<?> fallback = void.class;

	private Class<?> fallbackFactory = void.class;

	@Override
	public void afterPropertiesSet() {
		Assert.hasText(this.name, "Name must be set");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	protected ReactiveFeignBuilder reactiveFeign(ReactiveFeignContext context) {
		ReactiveFeignBuilder builder = get(context, ReactiveFeignBuilder.class)
				// required values
				.contract(get(context, Contract.class));

		builder = configureReactiveFeign(context, builder);

		return builder;
	}

	protected ReactiveFeignBuilder configureReactiveFeign(ReactiveFeignContext context, ReactiveFeignBuilder builder) {
		ReactiveFeignClientProperties properties = applicationContext.getBean(ReactiveFeignClientProperties.class);
		if (properties != null) {
			Map<String, ReactiveFeignClientProperties.ReactiveFeignClientConfiguration<?>> config = properties.getConfig();
			if (properties.isDefaultToProperties()) {
				configureUsingConfiguration(context, builder);
				configureUsingProperties(config.get(properties.getDefaultConfig()), builder);
				configureUsingProperties(config.get(this.name), builder);
			} else {
				configureUsingProperties(config.get(properties.getDefaultConfig()), builder);
				configureUsingProperties(config.get(this.name), builder);
				configureUsingConfiguration(context, builder);
			}
		} else {
			configureUsingConfiguration(context, builder);
		}

		for(ReactiveFeignConfigurator configurator : getAll(context, ReactiveFeignConfigurator.class).values()){
			builder = configurator.configure(builder, this, context);
		}
		return builder;
	}

	protected void configureUsingConfiguration(ReactiveFeignContext context, ReactiveFeignBuilder builder) {

		ReactiveRetryPolicy retryer = getOptional(context, ReactiveRetryPolicy.class);
		if (retryer != null) {
			builder.retryWhen(retryer);
		}
		ReactiveStatusHandler statusHandler = getOptional(context, ReactiveStatusHandler.class);
		if (statusHandler != null) {
			builder.statusHandler(statusHandler);
		}
		ReactiveOptions options = getOptional(context, ReactiveOptions.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, ReactiveHttpRequestInterceptor> requestInterceptors = context.getInstances(
				this.name, ReactiveHttpRequestInterceptor.class);
		if (requestInterceptors != null) {
			for(ReactiveHttpRequestInterceptor interceptor : requestInterceptors.values()){
				builder.addRequestInterceptor(interceptor);
			}
		}

		if (decode404) {
			builder.decode404();
		}
	}

	protected void configureUsingProperties(ReactiveFeignClientProperties.ReactiveFeignClientConfiguration<?> config,
											ReactiveFeignBuilder builder) {
		if (config == null) {
			return;
		}

		ReactiveOptions.Builder optionsBuilder = config.getOptions();
		if(optionsBuilder != null){
			builder.options(optionsBuilder.build());
		}

		if (config.getRetryPolicy() != null) {
			ReactiveRetryPolicy retryer = getOrInstantiate(config.getRetryPolicy());
			builder.retryWhen(retryer);
		}

		if (config.getStatusHandler() != null) {
			ReactiveStatusHandler errorDecoder = getOrInstantiate(config.getStatusHandler());
			builder.statusHandler(errorDecoder);
		}

		if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
			// this will add request interceptor to builder, not replace existing
			for (Class<ReactiveHttpRequestInterceptor> bean : config.getRequestInterceptors()) {
				ReactiveHttpRequestInterceptor interceptor = getOrInstantiate(bean);
				builder.addRequestInterceptor(interceptor);
			}
		}

		if (config.getDecode404() != null) {
			if (config.getDecode404()) {
				builder.decode404();
			}
		}

		if (Objects.nonNull(config.getContract())) {
			builder.contract(getOrInstantiate(config.getContract()));
		}
	}

	private <T> T getOrInstantiate(Class<T> tClass) {
		try {
			return applicationContext.getBean(tClass);
		} catch (NoSuchBeanDefinitionException e) {
			return BeanUtils.instantiateClass(tClass);
		}
	}

	protected <T> T get(ReactiveFeignContext context, Class<T> type) {
		T instance = context.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for "
					+ this.name);
		}
		return instance;
	}

	protected <T> Map<String, T> getAll(ReactiveFeignContext context, Class<T> type) {
		Map<String, T> instances = context.getInstances(this.name, type);
		return instances != null ? instances : emptyMap();
	}

	protected <T> T getOptional(ReactiveFeignContext context, Class<T> type) {
		return context.getInstance(this.name, type);
	}

	protected ReactiveFeignBuilder loadBalance(ReactiveFeignBuilder<?> builder, ReactiveFeignContext context) {


		return builder;
	}

	@Override
	public Object getObject() throws Exception {
		return getTarget();
	}

	/**
	 * @param <T> the target type of the Feign client
	 * @return a {@link ReactiveFeign} client created with the specified data and the context information
	 */
	private <T> T getTarget() {
		ReactiveFeignContext context = applicationContext.getBean(ReactiveFeignContext.class);
		ReactiveFeignBuilder builder = reactiveFeign(context);

		String url;
		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			}
			else {
				url = this.name;
			}
			builder = loadBalance(builder, context);
		} else {
			if(!this.url.startsWith("http")){
				url = "http://" + this.url;
			} else {
				url = this.url;
			}
		}
		url += cleanPath();

		builder = fallback(context, builder);

		return (T) builder.target(this.type, url);
	}

	private <T> ReactiveFeignBuilder fallback(ReactiveFeignContext context, ReactiveFeignBuilder builder) {
		if(fallback != void.class){
			Object fallbackInstance = getFromContext("fallback", getName(), context,
					this.fallback, this.type);
			builder = builder.fallback(fallbackInstance);
		}
		if(fallbackFactory != void.class){
			FallbackFactory<? extends T> fallbackFactoryInstance = (FallbackFactory<? extends T>)
					getFromContext("fallbackFactory", getName(), context, fallbackFactory, FallbackFactory.class);
		/* We take a sample fallback from the fallback factory to check if it returns a fallback
		that is compatible with the annotated feign interface. */
			Object exampleFallback = fallbackFactoryInstance.apply(new RuntimeException());
			Assert.notNull(exampleFallback,
					String.format(
							"Incompatible fallbackFactory instance for feign client %s. Factory may not produce null!",
							getName()));
			if (!this.type.isAssignableFrom(exampleFallback.getClass())) {
				throw new IllegalStateException(
						String.format(
								"Incompatible fallbackFactory instance for feign client %s. Factory produces instances of '%s', but should produce instances of '%s'",
								getName(), exampleFallback.getClass(), this.type));
			}
			builder = builder.fallbackFactory(fallbackFactoryInstance);
		}
		return builder;
	}

	private <T> T getFromContext(String fallbackMechanism, String feignClientName, ReactiveFeignContext context,
								 Class<?> beanType, Class<T> targetType) {
		Object fallbackInstance = context.getInstance(feignClientName, beanType);
		if (fallbackInstance == null) {
			throw new IllegalStateException(String.format(
					"No " + fallbackMechanism + " instance of type %s found for feign client %s",
					beanType, feignClientName));
		}

		if (!targetType.isAssignableFrom(beanType)) {
			throw new IllegalStateException(
					String.format(
							"Incompatible " + fallbackMechanism + " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
							beanType, targetType, feignClientName));
		}
		return (T) fallbackInstance;
	}

	private String cleanPath() {
		String path = this.path.trim();
		if (StringUtils.hasLength(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDecode404() {
		return decode404;
	}

	public void setDecode404(boolean decode404) {
		this.decode404 = decode404;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public Class<?> getFallback() {
		return fallback;
	}

	public void setFallback(Class<?> fallback) {
		this.fallback = fallback;
	}

	public Class<?> getFallbackFactory() {
		return fallbackFactory;
	}

	public void setFallbackFactory(Class<?> fallbackFactory) {
		this.fallbackFactory = fallbackFactory;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReactiveFeignClientFactoryBean that = (ReactiveFeignClientFactoryBean) o;
		return Objects.equals(applicationContext, that.applicationContext) &&
				decode404 == that.decode404 &&
				Objects.equals(fallback, that.fallback) &&
				Objects.equals(fallbackFactory, that.fallbackFactory) &&
				Objects.equals(name, that.name) &&
				Objects.equals(path, that.path) &&
				Objects.equals(type, that.type) &&
				Objects.equals(url, that.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicationContext, decode404, fallback, fallbackFactory,
				name, path, type, url);
	}

	@Override
	public String toString() {
		return new StringBuilder("ReactiveFeignClientFactoryBean{")
				.append("type=").append(type).append(", ")
				.append("name='").append(name).append("', ")
				.append("url='").append(url).append("', ")
				.append("path='").append(path).append("', ")
				.append("decode404=").append(decode404).append(", ")
				.append("applicationContext=").append(applicationContext).append(", ")
				.append("fallback=").append(fallback).append(", ")
				.append("fallbackFactory=").append(fallbackFactory)
				.append("}").toString();
	}

}
