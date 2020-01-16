package reactivefeign.cloud2;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import reactivefeign.ReactiveFeignBuilder;

@EnableAutoConfiguration
public class HystrixCircuitBreakerFuncTest extends reactivefeign.cloud.HystrixCircuitBreakerFuncTest{

    @Override
    protected ReactiveFeignBuilder<TestCaller> cloudBuilderWithTimeoutDisabledAndCircuitBreakerDisabled(){
        return BuilderUtils.cloudBuilder();
    }

}
