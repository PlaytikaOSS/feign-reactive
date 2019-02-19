package reactivefeign.cloud;

import com.netflix.client.ClientFactory;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

public interface ReactiveFeignClientFactory {

    ReactiveFeignClientFactory DEFAULT = new ReactiveFeignClientFactory() {};

    default ILoadBalancer loadBalancer(String name){
        return ClientFactory.getNamedLoadBalancer(name);
    }

    default IClientConfig clientConfig(String name){
        return DefaultClientConfigImpl.getClientConfigWithDefaultValues(name);
    }

}
