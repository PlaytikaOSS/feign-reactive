package reactivefeign.cloud;

import org.junit.BeforeClass;
import reactivefeign.ReactiveFeignBuilder;
import reactivefeign.cloud.common.AbstractPathVariableInTargetUrlTest;

import static reactivefeign.cloud.AllFeaturesTest.setupServersList;
import static reactivefeign.cloud.BuilderUtils.TEST_CLIENT_FACTORY;

public class PathVariableInTargetUrlTest extends AbstractPathVariableInTargetUrlTest {

    @BeforeClass
    public static void setUpServersList() {
        setupServersList(serviceName, server1.port());
    }

    @Override
    protected <T> ReactiveFeignBuilder<T> cloudBuilderWithLoadBalancerEnabled() {
        return BuilderUtils.<T>cloudBuilder()
                .enableLoadBalancer(TEST_CLIENT_FACTORY)
                .disableHystrix();
    }

}
