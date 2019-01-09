package reactivefeign.benchmarks;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

@Ignore
public class RealParallelRequestBenchmarksTest extends RealRequestBenchmarks{

    private ParallelRequestBenchmarks benchmarks;

    @Before
    public void before() throws Exception {
        benchmarks = new ParallelRequestBenchmarks();
        benchmarks.setup();
    }

    @After
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

