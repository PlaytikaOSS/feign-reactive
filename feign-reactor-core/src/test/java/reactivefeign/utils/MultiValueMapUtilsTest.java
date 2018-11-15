package reactivefeign.utils;


import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.utils.MultiValueMapUtils.*;

public class MultiValueMapUtilsTest {

    @Test
    public void shouldAddAllOrdered(){
        Map<String, List<String>> multiMap = new HashMap<>();
        addAllOrdered(multiMap, "key", singletonList("value1"));
        assertThat(multiMap.get("key")).containsExactly("value1");

        addAllOrdered(multiMap, "key", asList("value2", "value3"));
        assertThat(multiMap.get("key")).containsExactly("value1", "value2", "value3");
    }

    @Test
    public void shouldAddOrdered(){
        Map<String, List<String>> multiMap = new HashMap<>();
        addOrdered(multiMap, "key", "value1");
        assertThat(multiMap.get("key")).containsExactly("value1");

        addOrdered(multiMap, "key", "value2");
        assertThat(multiMap.get("key")).containsExactly("value1", "value2");
    }

    @Test
    public void shouldAddAll(){
        Map<String, Collection<String>> multiMap = new HashMap<>();
        addAll(multiMap, "key", singleton("value1"));
        assertThat(multiMap.get("key")).containsExactly("value1");

        addAll(multiMap, "key", new HashSet<>(asList("value2", "value3")));
        assertThat(multiMap.get("key")).containsExactlyInAnyOrder("value1", "value2", "value3");
    }

    @Test
    public void shouldAdd(){
        Map<String, Collection<String>> multiMap = new HashMap<>();
        add(multiMap, "key", "value1");
        assertThat(multiMap.get("key")).containsExactly("value1");

        add(multiMap, "key", "value2");
        assertThat(multiMap.get("key")).containsExactlyInAnyOrder("value1", "value2");
    }

}
