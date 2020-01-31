package reactivefeign.cloud;

import com.netflix.client.ClientException;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import org.junit.BeforeClass;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractLoadBalancingReactiveHttpClientTest;

import static org.assertj.core.api.Assertions.assertThat;
import static reactivefeign.cloud.AllFeaturesTest.setupServersList;
import static reactivefeign.cloud.BuilderUtils.TEST_CLIENT_FACTORY;

/**
 * @author Sergii Karpenko
 */
public class LoadBalancingReactiveHttpClientTest extends AbstractLoadBalancingReactiveHttpClientTest {

    @BeforeClass
    public static void setUpServersList() {
        setupServersList(serviceName, server1.port(), server2.port());
    }

    @Override
    protected <T> ReactiveFeignBuilder<T> cloudBuilderWithLoadBalancerEnabled() {
        return BuilderUtils.<T>cloudBuilder()
                .enableLoadBalancer(TEST_CLIENT_FACTORY)
                .disableHystrix();
    }

    @Override
    protected <T> ReactiveFeignBuilder<T> cloudBuilderWithLoadBalancerEnabled(
            int retryOnSame, int retryOnNext) {

        RetryHandler retryHandler = new RequestSpecificRetryHandler(true, true,
                new DefaultLoadBalancerRetryHandler(retryOnSame, retryOnNext, true), null);

        return BuilderUtils.<T>cloudBuilder()
                .enableLoadBalancer(TEST_CLIENT_FACTORY, retryHandler)
                .disableHystrix();
    }

    @Override
    protected boolean isOutOfRetries(Throwable t) {
        assertThat(t.getCause()).isInstanceOf(ClientException.class);
        assertThat(t.getCause().getMessage()).contains("Number of retries exceeded");
        return true;
    }

    @Override
    protected boolean isOutOutOfRetries(Throwable t) {
        assertThat(t.getCause()).isInstanceOf(ClientException.class);
        assertThat(t.getCause().getMessage()).contains("Number of retries on next server exceeded");
        return true;
    }

}
