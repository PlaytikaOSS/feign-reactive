package reactivefeign.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
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

@Configuration
@ConditionalOnClass(ReactiveFeign.class)
@AutoConfigureAfter(RibbonAutoConfiguration.class)
public class ReactiveFeignAutoConfiguration {

    @Autowired(required = false)
    private List<ReactiveFeignClientSpecification> configurations = new ArrayList<>();

    @Bean
    public HasFeatures reactiveFeignFeature() {
        return HasFeatures.namedFeature("ReactiveFeign", ReactiveFeign.class);
    }

    @Bean
    public ReactiveFeignContext reactiveFeignContext() {
        ReactiveFeignContext context = new ReactiveFeignContext();
        context.setConfigurations(this.configurations);
        return context;
    }

    @Configuration
    @ConditionalOnClass(WebReactiveFeign.class)
    public class WebClientReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientProperties<WebReactiveOptions.Builder> webClientReactiveFeignClientProperties() {
            return new ReactiveFeignClientProperties<>();
        }

    }

    @Configuration
    @ConditionalOnClass(Java11ReactiveFeign.class)
    public class Java11ReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientProperties<Java11ReactiveOptions.Builder> java11ReactiveFeignClientProperties() {
            return new ReactiveFeignClientProperties<>();
        }

    }

    @Configuration
    @ConditionalOnClass(JettyReactiveFeign.class)
    public class JettyReactiveFeignClientPropertiesAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("reactive.feign.client")
        public ReactiveFeignClientProperties<JettyReactiveOptions.Builder> jettyReactiveFeignClientProperties() {
            return new ReactiveFeignClientProperties<>();
        }

    }


}
