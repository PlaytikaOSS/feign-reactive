package reactivefeign.cloud;

import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

import java.util.function.Function;

public interface LoadBalancerCommandFactory extends Function<String, LoadBalancerCommand<Object>> {
}
