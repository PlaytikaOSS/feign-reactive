package reactivefeign.spring.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.DataBinder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class ReactiveFeignNamedContext {

    private final ApplicationContext applicationContext;
    private final ReactiveFeignNamedContextFactory namedContextFactory;
    private final String clientName;

    public ReactiveFeignNamedContext(ApplicationContext applicationContext, String clientName) {
        this.applicationContext = applicationContext;
        this.clientName = clientName;
        this.namedContextFactory = applicationContext.getBean(ReactiveFeignNamedContextFactory.class);
    }

    public <T> T get(Class<T> type) {
        T instance = namedContextFactory.getInstance(this.clientName, type);
        if (instance == null) {
            throw new IllegalStateException("No bean found of type " + type + " for "
                    + this.clientName);
        }
        return instance;
    }

    public <T> Map<String, T> getAll(Class<T> type) {
        Map<String, T> instances = namedContextFactory.getInstances(this.clientName, type);
        return instances != null ? instances : emptyMap();
    }

    public <T> T getOptional(Class<T> type) {
        return namedContextFactory.getInstance(this.clientName, type);
    }

    public <T> T getOptional(Class<T> type, String beanName) {
        Map<String, T> instances = namedContextFactory.getInstances(this.clientName, type);
        if(instances == null){
            return null;
        }
        return instances.get(beanName);
    }

    public String getClientName() {
        return clientName;
    }

    public List<ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>> getConfigs(){
        ReactiveFeignClientsProperties properties = getProperties();
        Map<String, ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>> config = properties.getConfig();
        return Arrays.asList(config.get(clientName), config.get(properties.getDefaultConfig()));
    }

    public List<ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>> getConfigsReverted(){
        ReactiveFeignClientsProperties properties = getProperties();
        Map<String, ReactiveFeignClientsProperties.ReactiveFeignClientProperties<?>> config = properties.getConfig();
        return Arrays.asList(config.get(properties.getDefaultConfig()), config.get(clientName));
    }

    public ReactiveFeignClientsProperties getProperties(){
        return applicationContext.getBean(ReactiveFeignClientsProperties.class);
    }

    public <T> T getOrInstantiate(Class<T> tClass) {
        try {
            return applicationContext.getBean(tClass);
        } catch (NoSuchBeanDefinitionException e) {
            return BeanUtils.instantiateClass(tClass);
        }
    }

    public <T> T getOrInstantiate(Class<T> tClass, Map args) {
        try {
            return applicationContext.getBean(tClass);
        } catch (NoSuchBeanDefinitionException e) {
            T bean = BeanUtils.instantiateClass(tClass);
            if(args != null && !args.isEmpty()) {
                DataBinder dataBinder = new DataBinder(bean);
                dataBinder.bind(new MutablePropertyValues(args));
                return (T) dataBinder.getTarget();
            } else {
                return bean;
            }
        }
    }
}
