package reactivefeign.cloud2;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractCircuitBreakerReactiveHttpClientTest;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud.LoadBalancingReactiveHttpClientTest.TestMonoInterface;

/**
 * @author Sergii Karpenko
 */
public class CircuitBreakerReactiveHttpClientTest extends AbstractCircuitBreakerReactiveHttpClientTest {

    private static CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private static ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory
            = new ReactiveResilience4JCircuitBreakerFactory();

    @BeforeClass
    public static void setupServersList() {
        circuitBreakerFactory.configureCircuitBreakerRegistry(circuitBreakerRegistry);
    }

    @Override
    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeoutDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .minimumNumberOfCalls(VOLUME_THRESHOLD)
                                .enableAutomaticTransitionFromOpenToHalfOpen()
                                .waitDurationInOpenState(Duration.ofMillis(SLEEP_WINDOW)).build())
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(Long.MAX_VALUE)).build()),
                lastCommandKey);
    }

    @Override
    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeout(int timeoutMs){
        return BuilderUtils.cloudBuilderWithUniqueCircuitBreaker(circuitBreakerFactory,
                configBuilder -> ((Resilience4JConfigBuilder)configBuilder)
                        .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(timeoutMs)).build()),
                lastCommandKey);
    }

    @Override
    protected void assertCircuitBreakerOpen(Throwable throwableCircuitOpened) {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Override
    protected boolean assertNoFallback(Throwable throwable) {
        return throwable instanceof NoFallbackAvailableException;
    }

    @Override
    protected boolean assertFailedAndFallbackFailed(Throwable throwable) {
        return throwable instanceof RuntimeException;
    }

    @Override
    protected void assertCircuitBreakerAllowRequests(Throwable throwable) {
        assertThat(throwable).isInstanceOf(NoFallbackAvailableException.class);
        assertThat(throwable.getCause()).isInstanceOf(RetryableException.class);
    }

    @Override
    protected void assertCircuitBreakerAllowRequests() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isIn(CircuitBreaker.State.CLOSED, CircuitBreaker.State.HALF_OPEN);
    }

    @Override
    protected void assertCircuitBreakerClosed() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isIn(CircuitBreaker.State.CLOSED);
    }

    @Override
    protected void assertCircuitBreakerOpen() {
        assertThat(circuitBreakerRegistry.circuitBreaker(lastCommandKey.get()).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Override
    protected boolean assertTimeout(Throwable throwable) {
        return throwable instanceof NoFallbackAvailableException
                && throwable.getCause() instanceof TimeoutException;
    }

    @Override
    protected void waitCircuitBreakerToAllowRequestAgain() {
        Awaitility.waitAtMost(Duration.ofMillis(SLEEP_WINDOW * 2))
                .pollDelay(Duration.ofMillis(UPDATE_INTERVAL))
                .untilAsserted(this::assertCircuitBreakerAllowRequests);
    }


}
