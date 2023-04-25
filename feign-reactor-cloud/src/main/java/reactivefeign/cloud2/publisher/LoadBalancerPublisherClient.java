package reactivefeign.cloud2.publisher;

import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.publisher.PublisherHttpClient;
import reactor.core.publisher.Flux;

import java.net.URI;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;

/**
 * @author Sergii Karpenko
 */
public class LoadBalancerPublisherClient implements PublisherHttpClient {

    private final ReactiveLoadBalancer<ServiceInstance> reactiveLoadBalancer;
    private final PublisherHttpClient publisherClient;

    public LoadBalancerPublisherClient(ReactiveLoadBalancer<ServiceInstance> reactiveLoadBalancer,
                                       PublisherHttpClient publisherClient) {
        this.reactiveLoadBalancer = reactiveLoadBalancer;
        this.publisherClient = publisherClient;
    }

    @Override
    public Publisher<Object> executeRequest(ReactiveHttpRequest request) {
        return Flux.from(reactiveLoadBalancer.choose())
                .flatMap(serviceInstanceResponse -> {
                    URI lbUrl = reconstructURI(serviceInstanceResponse.getServer(), request.uri());
                    ReactiveHttpRequest lbRequest = new ReactiveHttpRequest(request, lbUrl);
                    return publisherClient.executeRequest(lbRequest);
                });
    }
}
