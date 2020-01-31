package reactivefeign.cloud;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractCircuitBreakerFuncTest;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud.common.AbstractCircuitBreakerReactiveHttpClientTest.VOLUME_THRESHOLD;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {CircuitBreakerFuncTest.class},
        properties = {
                "hystrix.command.default.circuitBreaker.requestVolumeThreshold="+VOLUME_THRESHOLD,
                "hystrix.command.default.circuitBreaker.enabled=false",
                "hystrix.command.default.execution.timeout.enabled=false"
        })
@EnableAutoConfiguration
public class CircuitBreakerFuncTest extends AbstractCircuitBreakerFuncTest {
    private static final String CIRCUIT_IS_OPEN = "short-circuited";
    private static final String NO_FALLBACK = "and no fallback available";

    @Override
    protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled(){
        return BuilderUtils.cloudBuilder();
    }

    @Override
    protected void assertCircuitBreakerClosed(Throwable throwable) {
        assertThat(throwable).isInstanceOf(HystrixRuntimeException.class);
        assertThat(throwable.getMessage()).contains(NO_FALLBACK).doesNotContain(CIRCUIT_IS_OPEN);
    }

}
