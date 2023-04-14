package reactivefeign;

import org.junit.BeforeClass;
import org.junit.Test;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ServiceLoader;

abstract public class BaseReactorTest {

    public static final boolean INSTALL_BLOCKHOUND = true;

    public static final double BLOCKHOUND_DEGRADATION = 1.2;

    @BeforeClass
    public static void installBlockHound() {
        if (INSTALL_BLOCKHOUND) {
            BlockHound.Builder builder = BlockHound.builder();
            ServiceLoader<BlockHoundIntegration> serviceLoader = ServiceLoader.load(BlockHoundIntegration.class);
            serviceLoader.forEach(builder::with);

            //spring
            //check fails on server side as MimeTypeUtils$ConcurrentLruCache use this.lock.readLock().lock();
            builder.allowBlockingCallsInside("org.springframework.util.MimeTypeUtils", "parseMimeType");
            builder.allowBlockingCallsInside("org.springframework.util.MimeTypeUtils", "generateMultipartBoundary");
            //java.io.RandomAccessFile.readBytes
            builder.allowBlockingCallsInside("org.springframework.http.MediaTypeFactory", "parseMimeTypes");

            //reactor //missed in ReactorBlockHoundIntegration
            builder.allowBlockingCallsInside("java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue", "peek");
            builder.allowBlockingCallsInside("java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue", "remove");

            //netty
            builder.allowBlockingCallsInside("io.netty.resolver.DefaultHostsFileEntriesResolver", "parseEntries");
            builder.allowBlockingCallsInside("io.netty.util.concurrent.GlobalEventExecutor", "addTask");
            builder.allowBlockingCallsInside("io.netty.util.concurrent.GlobalEventExecutor", "takeTask");

            //jetty
            builder.allowBlockingCallsInside("org.eclipse.jetty.client.AbstractConnectionPool", "acquire");
            builder.allowBlockingCallsInside("org.eclipse.jetty.client.MultiplexConnectionPool", "acquire");
            builder.allowBlockingCallsInside("org.eclipse.jetty.client.MultiplexConnectionPool", "lock");

            builder.allowBlockingCallsInside("org.eclipse.jetty.util.BlockingArrayQueue", "poll");
            builder.allowBlockingCallsInside("org.eclipse.jetty.util.BlockingArrayQueue", "offer");
            builder.allowBlockingCallsInside("org.eclipse.jetty.util.BlockingArrayQueue", "peek");
            //java.net.InMemoryCookieStore.get
            builder.allowBlockingCallsInside("org.eclipse.jetty.client.HttpConnection", "normalizeRequest");
            builder.allowBlockingCallsInside("java.util.concurrent.FutureTask", "handlePossibleCancellationInterrupt");
            builder.allowBlockingCallsInside("org.eclipse.jetty.http2.HTTP2Session$StreamsState", "reserveSlot");
            builder.allowBlockingCallsInside("org.eclipse.jetty.http2.HTTP2Session$StreamsState", "flush");

            //jetty http2 server
            builder.allowBlockingCallsInside("org.eclipse.jetty.util.IteratingCallback", "processing");
            builder.allowBlockingCallsInside("org.eclipse.jetty.util.IteratingCallback", "iterate");

            //java11
            builder.allowBlockingCallsInside("jdk.internal.net.http.MultiExchange", "responseAsync");

            builder.allowBlockingCallsInside("com.sun.jmx.mbeanserver.Repository", "remove");
            builder.allowBlockingCallsInside("com.sun.jmx.mbeanserver.Repository", "contains");
            builder.allowBlockingCallsInside("com.sun.jmx.mbeanserver.Repository", "retrieve");
            builder.allowBlockingCallsInside("com.sun.jmx.mbeanserver.Repository", "addMBean");

            //Apache http client5
            builder.allowBlockingCallsInside("org.apache.hc.core5.pool.StrictConnPool", "lease");
            builder.allowBlockingCallsInside("org.apache.hc.core5.reactor.IOSessionImpl", "setEvent");

            builder.install();
        }
    }

    //by default, we want to detect blocking calls
    protected Scheduler testScheduler() {
        return Schedulers.parallel();
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailAsBlocking() {
        Mono.delay(Duration.ofSeconds(1))
                .doOnNext(it -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .block();
    }


}
