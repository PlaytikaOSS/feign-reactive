package reactivefeign.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactivefeign.ReactiveFeign;
import reactivefeign.java11.Java11ReactiveFeign;
import reactivefeign.java11.Java11ReactiveOptions;
import reactivefeign.jetty.JettyReactiveFeign;
import reactivefeign.jetty.JettyReactiveOptions;
import reactivefeign.webclient.WebReactiveFeign;
import reactivefeign.webclient.WebReactiveOptions;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration(after = LoadBalancerAutoConfiguration.class)
@ConditionalOnClass(ReactiveFeign.class)
public class ReactiveFeignAutoConfiguration {

    @Autowired(required = false)
    private List<ReactiveFeignClientSpecification> configurations = new ArrayList<>();

    @Bean
    public HasFeatures reactiveFeignFeature() {
        return HasFeatures.namedFeature("ReactiveFeign", ReactiveFeign.class);
    }

    @Bean
    public ReactiveFeignNamedContextFactory reactiveFeignContext() {
        ReactiveFeignNamedContextFactory context = new ReactiveFeignNamedContextFactory();
        context.setConfigurations(this.configurations);
        return context;
    }

    @Configuration
    @ConditionalOnClass(WebReactiveFeign.class)
    public class WebClientReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientsProperties<WebReactiveOptions.Builder> webClientReactiveFeignClientProperties() {
            return new ReactiveFeignClientsProperties<>();
        }

    }

    @Configuration
    @ConditionalOnClass(Java11ReactiveFeign.class)
    public class Java11ReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientsProperties<Java11ReactiveOptions.Builder> java11ReactiveFeignClientProperties() {
            return new ReactiveFeignClientsProperties<>();
        }

    }

    @Configuration
    @ConditionalOnClass(JettyReactiveFeign.class)
    public class JettyReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientsProperties<JettyReactiveOptions.Builder> jettyReactiveFeignClientProperties() {
            return new ReactiveFeignClientsProperties<>();
        }

    }

}
