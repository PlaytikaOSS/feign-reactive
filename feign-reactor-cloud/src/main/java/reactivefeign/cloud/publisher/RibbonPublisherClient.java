package reactivefeign.cloud.publisher;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import org.reactivestreams.Publisher;
import org.springframework.web.util.UriComponentsBuilder;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.cloud.LoadBalancerCommandFactory;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.utils.LazyInitialized;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import java.lang.reflect.Type;
import java.net.URI;

/**
 * @author Sergii Karpenko
 */
public class RibbonPublisherClient implements PublisherHttpClient {

    private final LazyInitialized<LoadBalancerCommand<Object>> loadBalancerCommand;
    private final PublisherHttpClient publisherClient;
    private final Type publisherType;

    public RibbonPublisherClient(LoadBalancerCommandFactory loadBalancerCommandFactory,
                                 String serviceName,
                                 PublisherHttpClient publisherClient,
                                 Type publisherType) {
        this.loadBalancerCommand = new LazyInitialized<>(() -> loadBalancerCommandFactory.apply(serviceName));
        this.publisherClient = publisherClient;
        this.publisherType = publisherType;
    }

    @Override
    public Publisher<Object> executeRequest(ReactiveHttpRequest request) {
        LoadBalancerCommand<Object> loadBalancerCommand = this.loadBalancerCommand.get();
        if (loadBalancerCommand != null) {
            Observable<?> observable = loadBalancerCommand.submit(server -> {

                ReactiveHttpRequest lbRequest = loadBalanceRequest(request, server);

                Publisher<Object> publisher = publisherClient.executeRequest(lbRequest);
                return RxReactiveStreams.toObservable(publisher);
            });

            Publisher<?> publisher = RxReactiveStreams.toPublisher(observable);

            if(publisherType == Mono.class){
                return Mono.from(publisher);
            } else if(publisherType == Flux.class){
                return Flux.from(publisher);
            } else {
                throw new IllegalArgumentException("Unknown publisherType: " + publisherType);
            }
        } else {
            return publisherClient.executeRequest(request);
        }
    }

    protected ReactiveHttpRequest loadBalanceRequest(ReactiveHttpRequest request, Server server) {
        URI lbUrl = UriComponentsBuilder.fromUri(request.uri())
                .host(server.getHost())
                .port(server.getPort())
                .build(true).toUri();
        return new ReactiveHttpRequest(request.method(), lbUrl, request.headers(), request.body());
    }
}
