package reactivefeign.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.utils.StringUtils.cutTail;

public class StringUtilsTest {

    @Test
    public void shouldCutTail(){
        assertThat(cutTail("1234", "34")).isEqualTo("12");
    }
}
