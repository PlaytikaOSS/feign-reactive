package reactivefeign.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.utils.HttpUtils.StatusCodeFamily.*;
import static reactivefeign.utils.HttpUtils.familyOf;

public class HttpUtilsTest {

    @Test
    public void should(){
        assertThat(familyOf(100)).isEqualTo(INFORMATIONAL);
        assertThat(familyOf(199)).isEqualTo(INFORMATIONAL);
        assertThat(familyOf(300)).isEqualTo(REDIRECTION);
        assertThat(familyOf(399)).isEqualTo(REDIRECTION);
        assertThat(familyOf(99)).isEqualTo(OTHER);
        assertThat(familyOf(600)).isEqualTo(OTHER);
        assertThat(familyOf(700)).isEqualTo(OTHER);
    }

}

