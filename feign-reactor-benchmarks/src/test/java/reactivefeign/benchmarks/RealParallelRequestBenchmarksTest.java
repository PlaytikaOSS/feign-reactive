package reactivefeign.benchmarks;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

@Ignore
public class RealParallelRequestBenchmarksTest extends RealRequestBenchmarks{

    private RealParallelRequestBenchmarks benchmarks;

    @Before
    public void before() throws Exception {
        benchmarks = new RealParallelRequestBenchmarks();
        benchmarks.setup();
    }

    @After
    public void after() throws Exception {
        benchmarks.tearDown();
    }

    @Test
    public void testFeignEmptyPayload() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 100; i++) benchmarks.feignEmptyPayload();
    }

    @Test
    public void testWebClientWithPayload(){
        for (int i = 0; i < 100; i++) benchmarks.webClient();
    }

    @Test
    public void testFeignWebClientWithPayload(){
        for (int i = 0; i < 100; i++) benchmarks.feignWebClient();
    }

    @Test
    public void testFeignJettyWithPayload(){
        for (int i = 0; i < 100; i++) benchmarks.feignJetty();
    }

    @Test
    public void testFeignWithPayload() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 100; i++) benchmarks.feign();
    }
}

