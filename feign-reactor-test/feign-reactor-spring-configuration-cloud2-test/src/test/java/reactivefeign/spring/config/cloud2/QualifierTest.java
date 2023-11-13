package reactivefeign.spring.config.cloud2;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static reactivefeign.spring.config.cloud2.BasicAutoconfigurationTest.MOCK_SERVER_PORT_PROPERTY;
import static reactivefeign.spring.config.cloud2.QualifierTest.RFGN_PROPS;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = QualifierTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.discovery.client.simple.instances."+RFGN_PROPS+"[0].uri=http://localhost:${"+ MOCK_SERVER_PORT_PROPERTY+"}",
                "reactive.feign.client.config.default.options.readTimeoutMillis=100",
                "reactive.feign.client.config.rfgn-proper.options.readTimeoutMillis=500"
        })
@DirtiesContext
public class QualifierTest extends BasicAutoconfigurationTest{

    public static final String RFGN_PROPS = "rfgn-proper";

    private static final WireMockServer mockHttpServer = new WireMockServer(wireMockConfig().dynamicPort());

    @Autowired
    private PropsSampleClient propsSampleClient;

    @Autowired
    private PropsSampleClientWithOtherQualifier propsSampleClientWithOtherQualifier;


    @Test
    public void shouldRunClientsWithSameNameAndDifferentQualifiers() {
        mockHttpServer.stubFor(get(urlPathMatching("/sampleUrl"))
                .willReturn(aResponse()
                        .withFixedDelay(300)
                        .withBody("OK")));

        asList(propsSampleClient, propsSampleClientWithOtherQualifier).forEach(feignClient -> {
            StepVerifier.create(feignClient.sampleMethod())
                    .expectNext("OK")
                    .verifyComplete();
        });
    }

    @ReactiveFeignClient(name = RFGN_PROPS, qualifier = "FeignClient1")
    protected interface PropsSampleClient extends SampleClient {

        @RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
        Mono<String> sampleMethod();
    }

    @ReactiveFeignClient(name = RFGN_PROPS, qualifier = "FeignClient2")
    protected interface PropsSampleClientWithOtherQualifier extends SampleClient {

        @RequestMapping(method = RequestMethod.GET, value = "/sampleUrl")
        Mono<String> sampleMethod();
    }

    protected interface SampleClient{
        Mono<String> sampleMethod();
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableReactiveFeignClients(
            clients = {PropsSampleClient.class,
                    PropsSampleClientWithOtherQualifier.class})
    protected static class TestConfiguration {
    }

    @BeforeClass
    public static void setupStubs() {
        mockHttpServer.start();

        System.setProperty(MOCK_SERVER_PORT_PROPERTY, Integer.toString(mockHttpServer.port()));
    }

    @Before
    public void reset() throws InterruptedException {
        //to close circuit breaker
        mockHttpServer.resetAll();
    }

    @AfterClass
    public static void teardown() {
        mockHttpServer.stop();
    }
}
