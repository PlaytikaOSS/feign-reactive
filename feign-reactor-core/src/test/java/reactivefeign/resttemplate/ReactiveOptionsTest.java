package reactivefeign.resttemplate;

import org.junit.Test;
import reactivefeign.resttemplate.client.RestTemplateReactiveOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.ReactiveOptions.useHttp2;

public class ReactiveOptionsTest {

    @Test
    public void shouldNotUseHttp2ByDefault(){
        assertThat(useHttp2(new RestTemplateReactiveOptions.Builder().build())).isFalse();
    }

    @Test
    public void shouldUseHttp2(){
        assertThat(useHttp2(new RestTemplateReactiveOptions.Builder().setUseHttp2(true).build())).isTrue();
    }

}
