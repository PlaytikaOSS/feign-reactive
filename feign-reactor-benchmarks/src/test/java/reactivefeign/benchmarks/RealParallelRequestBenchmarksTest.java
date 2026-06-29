package reactivefeign.benchmarks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

@Disabled
public class RealParallelRequestBenchmarksTest extends RealRequestBenchmarks{

    private ParallelRequestBenchmarks benchmarks;

    @BeforeEach
    public void before() throws Exception {
        benchmarks = new ParallelRequestBenchmarks();
        benchmarks.setup();
    }

    @AfterEach
    public void after() throws Exception {
        benchmarks.tearDown();
    }

    @Test
    public void testWebClientWithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.webClient();
    }

    @Test
    public void testFeignWebClientWithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.feignWebClient();
    }

    @Test
    public void testFeignJettyWithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.feignJetty();
    }

    @Test
    public void testFeignJettyH2cWithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.feignJettyH2c();
    }

    @Test
    public void testFeignJava11WithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.feignJava11();
    }

    @Test
    public void testFeignJava11H2cWithPayload(){
        for (int i = 0; i < 10; i++) benchmarks.feignJava11H2c();
    }

    @Test
    public void testFeignWithPayload() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 10; i++) benchmarks.feign();
    }
}

