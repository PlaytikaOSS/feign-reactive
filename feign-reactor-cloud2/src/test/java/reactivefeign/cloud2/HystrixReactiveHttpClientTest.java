package reactivefeign.cloud2;

import reactivefeign.ReactiveFeignBuilder;

import static reactivefeign.cloud.LoadBalancingReactiveHttpClientTest.TestMonoInterface;

/**
 * @author Sergii Karpenko
 */
public class HystrixReactiveHttpClientTest extends reactivefeign.cloud.HystrixReactiveHttpClientTest{

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


}
