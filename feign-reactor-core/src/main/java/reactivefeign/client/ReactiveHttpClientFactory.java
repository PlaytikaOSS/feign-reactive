package reactivefeign.client;

import feign.MethodMetadata;
import feign.Target;

public interface ReactiveHttpClientFactory {

    default void target(Target target){}

    ReactiveHttpClient create(MethodMetadata methodMetadata);

}
