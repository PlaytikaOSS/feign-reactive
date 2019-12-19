package reactivefeign.cloud;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Param;
import feign.RequestLine;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonList;

public class PathVariableInTargetUrlTest {

    @ClassRule
    public static WireMockClassRule server1 = new WireMockClassRule(wireMockConfig().dynamicPort());

    private static String serviceName = "PathVariableInTargetUrlTest";

    @BeforeClass
    public static void setupServersList() throws ClientException {
        DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl();
        clientConfig.loadDefaultValues();
        clientConfig.setProperty(CommonClientConfigKey.NFLoadBalancerClassName, BaseLoadBalancer.class.getName());
        ILoadBalancer lb = ClientFactory.registerNamedLoadBalancerFromclientConfig(serviceName, clientConfig);
        lb.addServers(singletonList(new Server("localhost", server1.port())));
    }

    @Before
    public void resetServers() {
        server1.resetAll();
    }

    @Test
    public void shouldCorrectlyProcessPathVariableInUrl(){

        String body = "Success";
        mockSuccessMono(server1, body);

        TestMonoInterface client = BuilderUtils.<TestMonoInterface>cloudBuilder("shouldCorrectlyProcessPathVariableInUrl")
                .enableLoadBalancer()
                .disableHystrix()
                .target(TestMonoInterface.class, serviceName, "http://"+serviceName+"/mono/{id}");

        StepVerifier.create(client.getMono(1))
                .expectNext(body)
                .verifyComplete();
    }

    static void mockSuccessMono(WireMockClassRule server, String body) {
        server.stubFor(get(urlPathMatching("/mono/1/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    interface TestMonoInterface {

        @RequestLine("GET")
        Mono<String> getMono(@Param("id") long id);
    }
}
