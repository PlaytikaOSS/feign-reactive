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
import reactor.util.context.Context;
import rx.Observable;
import rx.RxReactiveStreams;

import java.lang.reflect.Type;
import java.net.URI;

import static reactivefeign.cloud.SubscriberContextUtils.withContext;

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
            if(publisherType == Mono.class){
                return Mono.subscriberContext()
                        .flatMap(context -> Mono.from(
                                getLoadBalancedPublisher(request, loadBalancerCommand, context)));
            } else if(publisherType == Flux.class){
                return Mono.subscriberContext()
                        .flatMapMany(context -> Flux.from(
                                getLoadBalancedPublisher(request, loadBalancerCommand, context)));
            } else {
                throw new IllegalArgumentException("Unknown publisherType: " + publisherType);
            }
        } else {
            return publisherClient.executeRequest(request);
        }
    }

    private Publisher<?> getLoadBalancedPublisher(
            ReactiveHttpRequest request,
            LoadBalancerCommand<Object> loadBalancerCommand,
            Context context) {
        Observable<?> observable = loadBalancerCommand.submit(server -> {

            ReactiveHttpRequest lbRequest = loadBalanceRequest(request, server);

            Publisher<Object> publisher = publisherClient.executeRequest(lbRequest);
            return RxReactiveStreams.toObservable(withContext(publisher, publisherType, context));
        });

        return RxReactiveStreams.toPublisher(observable);
    }

    protected ReactiveHttpRequest loadBalanceRequest(ReactiveHttpRequest request, Server server) {
        URI lbUrl = UriComponentsBuilder.fromUri(request.uri())
                .host(server.getHost())
                .port(server.getPort())
                .build(true).toUri();
        return new ReactiveHttpRequest(request, lbUrl);
    }
}
