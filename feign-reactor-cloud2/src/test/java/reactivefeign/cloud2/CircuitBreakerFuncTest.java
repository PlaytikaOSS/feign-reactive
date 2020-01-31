package reactivefeign.cloud2;


import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractCircuitBreakerFuncTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = reactivefeign.cloud2.CircuitBreakerFuncTest.class)
@EnableAutoConfiguration
public class CircuitBreakerFuncTest extends AbstractCircuitBreakerFuncTest {

    private static ReactiveCircuitBreakerFactory circuitBreakerFactory;

    @BeforeClass
    public static void setupServersList() {
        circuitBreakerFactory = new ReactiveResilience4JCircuitBreakerFactory();
    }

    @Override
    protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(Long.MAX_VALUE)).build())
                        .circuitBreakerConfig(CircuitBreakerConfig.custom().minimumNumberOfCalls(Integer.MAX_VALUE).build()),
                null);
    }

    @Override
    protected void assertCircuitBreakerClosed(Throwable throwable) {
        assertThat(throwable).isInstanceOf(NoFallbackAvailableException.class);
    }

}
