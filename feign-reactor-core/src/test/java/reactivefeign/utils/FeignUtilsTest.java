package reactivefeign.utils;

import org.junit.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FeignUtilsTest {

    public static Mono<String> testMono;

    public static List<String> testList;

    @Test
    public void shouldReturnPublisherParameter() throws NoSuchFieldException {
        Type bodyActualType = FeignUtils.getBodyActualType(
                FeignUtilsTest.class.getField("testMono").getGenericType());
        assertThat(bodyActualType).isEqualTo(String.class);
    }

    @Test
    public void shouldReturnPassedParameterIfNotPublisher() throws NoSuchFieldException {
        Type bodyActualType = FeignUtils.getBodyActualType(
                FeignUtilsTest.class.getField("testList").getGenericType());
        assertThat(((ParameterizedType)bodyActualType).getRawType()).isEqualTo(List.class);
    }

    @Test
    public void shouldReturnPassedParameterIfNotParametrized(){
        Type bodyActualType = FeignUtils.getBodyActualType(String.class);
        assertThat(bodyActualType).isEqualTo(String.class);
    }
}
