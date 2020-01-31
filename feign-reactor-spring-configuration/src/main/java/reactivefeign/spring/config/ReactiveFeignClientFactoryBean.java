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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactivefeign.FallbackFactory;
import reactivefeign.ReactiveFeign;
import reactivefeign.ReactiveFeignBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

	@Override
	public Object getObject() {
		return getTarget();
	}

	/**
	 * @param <T> the target type of the Feign client
	 * @return a {@link ReactiveFeign} client created with the specified data and the context information
	 */
	private <T> T getTarget() {
		ReactiveFeignNamedContext namedContext = new ReactiveFeignNamedContext(applicationContext, name);
		ReactiveFeignBuilder builder = namedContext.get(ReactiveFeignBuilder.class)
				.contract(namedContext.get(Contract.class));

		builder = applyConfigurators(builder, namedContext);

		if (decode404) {
			builder = builder.decode404();
		}

		builder = fallback(builder, namedContext);

		return StringUtils.hasText(this.url)
				? (T) builder.target(type, buildUrl())
				: (T) builder.target(type, this.name, buildUrl());
	}

	protected ReactiveFeignBuilder applyConfigurators(ReactiveFeignBuilder builder, ReactiveFeignNamedContext namedContext) {
		List<ReactiveFeignConfigurator> configurators = new ArrayList<>(
				namedContext.getAll(ReactiveFeignConfigurator.class).values());
		Collections.sort(configurators);

		for (ReactiveFeignConfigurator configurator : configurators) {
			builder = configurator.configure(builder, namedContext);
		}

		return builder;
	}

	private String buildUrl() {
		String url;
		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			}
			else {
				url = this.name;
			}
		} else {
			if(!this.url.startsWith("http")){
				url = "http://" + this.url;
			} else {
				url = this.url;
			}
		}
		url += cleanPath();
		return url;
	}

	private <T> ReactiveFeignBuilder fallback(ReactiveFeignBuilder builder, ReactiveFeignNamedContext context) {
		if(fallback != void.class){
			Object fallbackInstance = getFallbackFromContext(
					"fallback", context, this.fallback, this.type);
			builder = builder.fallback(fallbackInstance);
			return builder;
		}
		if(fallbackFactory != void.class){
			FallbackFactory<? extends T> fallbackFactoryInstance = (FallbackFactory<? extends T>)getFallbackFromContext(
							"fallbackFactory", context, fallbackFactory, FallbackFactory.class);
			builder = builder.fallbackFactory(fallbackFactoryInstance);
			return builder;
		}

		FallbackFactory<? extends T> fallbackFactoryInstance = context.getOptional(FallbackFactory.class);
		if(fallbackFactoryInstance != null) {
			builder = builder.fallbackFactory(fallbackFactoryInstance);
		}
		return builder;
	}

	private <T> T getFallbackFromContext(String fallbackMechanism, ReactiveFeignNamedContext context,
										 Class<?> beanType, Class<T> targetType) {
		Object fallbackInstance = context.getOptional(beanType);
		if (!targetType.isAssignableFrom(beanType)) {
			throw new IllegalStateException(
					String.format(
							"Incompatible " + fallbackMechanism + " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
							beanType, targetType, context.getClientName()));
		}
		if(fallbackInstance == null){
			fallbackInstance = context.getOrInstantiate(beanType);
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
