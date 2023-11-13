/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.CoreMatchers.is;
import static reactor.netty.http.HttpProtocol.HTTP11;

/**
 * @author Sergii Karpenko
 */
abstract public class ReactivityTest extends BaseReactorTest {

  public static final int DELAY_IN_MILLIS = 500;
  public static final int CALLS_NUMBER = 500;
  public static final int REACTIVE_GAIN_RATIO = 30;

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  private static DisposableServer server;

  @Before
  public void startServer() throws JsonProcessingException {
    if(server == null) {
      byte[] data = TestUtils.MAPPER.writeValueAsString(new OrderGenerator().generate(1)).getBytes();

      server = HttpServer.create()
              .protocol(serverProtocols())
              .http2Settings(builder -> builder.maxConcurrentStreams(1000))
              .route(r -> r.get("/icecream/orders/1",
                      (req, res) -> {
                        res.header("Content-Type", "application/json");
                        return Mono.delay(Duration.ofMillis(DELAY_IN_MILLIS))
                                .thenEmpty(res.sendByteArray(Mono.just(data)));
                      }))
              .bindNow();
    }
  }

  protected HttpProtocol[] serverProtocols(){
    return new HttpProtocol[]{HTTP11};
  }

  @AfterClass
  public static void stopServer(){
    server.disposeNow();
    server = null;
  }

  @Test
  public void shouldRunReactively() throws JsonProcessingException {

    IcecreamServiceApi client = builder()
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + server.port());

    AtomicInteger counter = new AtomicInteger();

    new Thread(() -> {
      for (int i = 0; i < CALLS_NUMBER; i++) {
        client.findFirstOrder().subscribeOn(testScheduler())
                .doOnNext(order -> counter.incrementAndGet())
                .subscribe();
      }
    }).start();

    waitAtMost(timeToCompleteReactively(), TimeUnit.MILLISECONDS)
            .untilAtomic(counter, is(CALLS_NUMBER));
  }

  public static int timeToCompleteReactively(){
    return CALLS_NUMBER * DELAY_IN_MILLIS
            / (INSTALL_BLOCKHOUND
            ? (int)(REACTIVE_GAIN_RATIO / BLOCKHOUND_DEGRADATION)
            : REACTIVE_GAIN_RATIO);
  }
}
