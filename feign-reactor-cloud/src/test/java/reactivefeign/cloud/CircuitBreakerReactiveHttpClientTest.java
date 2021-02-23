package reactivefeign.cloud;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractCircuitBreakerReactiveHttpClientTest;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud.LoadBalancingReactiveHttpClientTest.TestMonoInterface;

/**
 * @author Sergii Karpenko
 */
public class CircuitBreakerReactiveHttpClientTest extends AbstractCircuitBreakerReactiveHttpClientTest {

    @Override
    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeoutDisabled(){
        return BuilderUtils.cloudBuilderWithUniqueHystrixCommand(
                hystrixPropertiesTimeoutDisabled(), lastCommandKey);
    }

    @Override
    protected ReactiveFeignBuilder<TestMonoInterface> cloudBuilderWithTimeout(int timeoutMs){
        return BuilderUtils.cloudBuilderWithUniqueHystrixCommand(
                hystrixProperties()
                        .withExecutionTimeoutEnabled(true)
                        .withExecutionTimeoutInMilliseconds(timeoutMs), lastCommandKey);
    }

    @Override
    protected boolean assertNoFallback(Throwable throwable) {
        return throwable instanceof HystrixRuntimeException
                && throwable.getMessage().contains("failed and no fallback available");
    }

    @Override
    protected void assertCircuitBreakerOpen(Throwable throwableCircuitOpened) {
        assertThat(throwableCircuitOpened).isInstanceOf(HystrixRuntimeException.class);
        assertThat(throwableCircuitOpened.getMessage())
                .contains("short-circuited and no fallback available.");
    }

    @Override
    protected boolean assertFailedAndFallbackFailed(Throwable throwable) {
        return throwable instanceof HystrixRuntimeException
                && throwable.getMessage().contains("failed and fallback failed");
    }

    @Override
    protected void waitCircuitBreakerToAllowRequestAgain() {
        try {
            Thread.sleep(SLEEP_WINDOW);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void assertCircuitBreakerAllowRequests(Throwable throwable) {
        assertThat(throwable).isInstanceOf(HystrixRuntimeException.class);
        assertThat(throwable.getMessage())
                .contains("and no fallback available")
                .doesNotContain("short-circuited");
    }

    @Override
    protected void assertCircuitBreakerAllowRequests() {
    }

    @Override
    protected void assertCircuitBreakerClosed() {
        assertThat(HystrixCircuitBreaker.Factory.getInstance(
                HystrixCommandKey.Factory.asKey(lastCommandKey.get()))
                .isOpen())
                .isFalse();
    }

    @Override
    protected void assertCircuitBreakerOpen() {
        assertThat(HystrixCircuitBreaker.Factory.getInstance(
                HystrixCommandKey.Factory.asKey(lastCommandKey.get()))
                .isOpen())
                .isTrue();
    }

    @Override
    protected boolean assertTimeout(Throwable throwable) {
        return throwable instanceof HystrixRuntimeException
                && throwable.getCause() instanceof TimeoutException;
    }

    private static HystrixCommandProperties.Setter hystrixProperties(){
        return HystrixCommandProperties.Setter()
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerRequestVolumeThreshold(VOLUME_THRESHOLD)
                .withCircuitBreakerSleepWindowInMilliseconds(SLEEP_WINDOW)
                .withMetricsRollingStatisticalWindowInMilliseconds(1000)
                .withMetricsRollingStatisticalWindowBuckets(1)
                .withMetricsHealthSnapshotIntervalInMilliseconds(UPDATE_INTERVAL);
    }

    private static HystrixCommandProperties.Setter hystrixPropertiesTimeoutDisabled(){
        return hystrixProperties()
                .withExecutionTimeoutEnabled(false);
    }

}