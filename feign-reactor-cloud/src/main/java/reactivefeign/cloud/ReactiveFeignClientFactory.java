package reactivefeign.cloud;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

public interface ReactiveFeignClientFactory {

    ILoadBalancer loadBalancer(String name);

    IClientConfig clientConfig(String name);

}
