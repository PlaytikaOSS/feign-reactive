package reactivefeign.utils;

import java.util.Collection;

public class CollectionUtils {

    public static <E> boolean isEmpty(Collection<E> c){
        return c == null || c.isEmpty();
    }
}
