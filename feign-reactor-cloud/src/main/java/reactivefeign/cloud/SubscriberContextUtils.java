package reactivefeign.cloud;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.lang.reflect.Type;

public class SubscriberContextUtils {

    public static  <T extends Publisher> T withContext(T publisher, Type returnPublisherType, Context context) {
        if (returnPublisherType == Mono.class) {
            return (T) ((Mono) publisher).subscriberContext(context);
        } else {
            return (T) ((Flux) publisher).subscriberContext(context);
        }
    }


}
