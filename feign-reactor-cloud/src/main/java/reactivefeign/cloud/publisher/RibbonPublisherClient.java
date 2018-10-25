package reactivefeign.cloud.publisher;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import org.reactivestreams.Publisher;
import org.springframework.lang.Nullable;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.publisher.PublisherHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Sergii Karpenko
 */
public class RibbonPublisherClient implements PublisherHttpClient {

    private final LoadBalancerCommand<Object> loadBalancerCommand;
    private final PublisherHttpClient publisherClient;
    private final Type publisherType;

    public RibbonPublisherClient(@Nullable LoadBalancerCommand<Object> loadBalancerCommand,
                                 PublisherHttpClient publisherClient,
                                 Type publisherType) {
        this.loadBalancerCommand = loadBalancerCommand;
        this.publisherClient = publisherClient;
        this.publisherType = publisherType;
    }

    @Override
    public Publisher<?> executeRequest(ReactiveHttpRequest request) {

        if (loadBalancerCommand != null) {
            Observable<?> observable = loadBalancerCommand.submit(server -> {

                ReactiveHttpRequest lbRequest = loadBalanceRequest(request, server);

                Publisher<Object> publisher = (Publisher<Object>)publisherClient.executeRequest(lbRequest);
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
        URI uri = request.uri();
        try {
            URI lbUrl = new URI(uri.getScheme(), uri.getUserInfo(), server.getHost(), server.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
            return new ReactiveHttpRequest(request.method(), lbUrl, request.headers(), request.body());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
