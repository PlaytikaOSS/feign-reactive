package reactivefeign.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactivefeign.ReactiveFeign;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass(ReactiveFeign.class)
@EnableConfigurationProperties({ReactiveFeignClientProperties.class})
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

//    @Configuration
//    @ConditionalOnClass(name = "feign.hystrix.HystrixFeign")
//    protected static class HystrixFeignTargeterConfiguration {
//        @Bean
//        @ConditionalOnMissingBean
//        public Targeter feignTargeter() {
//            return new HystrixTargeter();
//        }
//    }
//
//    @Configuration
//    @ConditionalOnMissingClass("feign.hystrix.HystrixFeign")
//    protected static class DefaultFeignTargeterConfiguration {
//        @Bean
//        @ConditionalOnMissingBean
//        public Targeter feignTargeter() {
//            return new DefaultTargeter();
//        }
//    }

}
