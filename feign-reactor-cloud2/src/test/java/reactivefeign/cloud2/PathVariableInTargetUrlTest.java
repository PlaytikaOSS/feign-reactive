package reactivefeign.cloud2;

import org.junit.BeforeClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.ReactiveFeignBuilder;

public class PathVariableInTargetUrlTest extends reactivefeign.cloud.PathVariableInTargetUrlTest{

    private static ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    @BeforeClass
    public static void setupServersList() {
        loadBalancerFactory = LoadBalancingReactiveHttpClientTest.loadBalancerFactory(serviceName, server1.port());
    }

    @Override
    protected <T> ReactiveFeignBuilder<T> cloudBuilderWithLoadBalancerEnabled() {
        return BuilderUtils.<T>cloudBuilder()
                .enableLoadBalancer(loadBalancerFactory)
                .disableHystrix();
    }

}
