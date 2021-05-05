package reactivefeign.cloud2;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.Param;
import feign.RequestLine;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import reactivefeign.BaseReactorTest;
import reactivefeign.ReactiveFeignBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class PathVariableInTargetUrlTest extends BaseReactorTest {

    @ClassRule
    public static WireMockClassRule server1 = new WireMockClassRule(wireMockConfig().dynamicPort());

    protected static String serviceName = "PathVariableInTargetUrlTest";

    private static ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    @BeforeClass
    public static void setupServersList() {
        loadBalancerFactory = LoadBalancingReactiveHttpClientTest.loadBalancerFactory(serviceName, server1.port());
    }

    @Before
    public void resetServers() {
        server1.resetAll();
    }

    @Test
    public void shouldCorrectlyProcessPathVariableInUrl(){

        String body = "Success";
        mockSuccessMono(server1, body);

        TestMonoInterface client = this.<TestMonoInterface>cloudBuilderWithLoadBalancerEnabled()
                .target(TestMonoInterface.class, serviceName, "http://"+serviceName+"/mono/{id}");

        StepVerifier.create(client.getMono(1).subscribeOn(testScheduler()))
                .expectNext(body)
                .verifyComplete();
    }

    static void mockSuccessMono(WireMockClassRule server, String body) {
        server.stubFor(get(urlPathMatching("/mono/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    interface TestMonoInterface {

        @RequestLine("GET")
        Mono<String> getMono(@Param("id") long id);
    }

    protected <T> ReactiveFeignBuilder<T> cloudBuilderWithLoadBalancerEnabled() {
        return BuilderUtils.<T>cloudBuilder()
                .enableLoadBalancer(loadBalancerFactory);
    }

}
