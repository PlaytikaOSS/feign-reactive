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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactivefeign.testcase.IcecreamServiceApi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sergii Karpenko
 */
abstract public class ConnectionTimeoutTest extends BaseReactorTest{

  private ServerSocket serverSocket;
  private Socket socket;
  private int port;

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long connectTimeoutInMillis);

  @BeforeEach
  public void before() throws IOException {
    // server socket with single element backlog queue (1) and dynamicaly allocated
    // port (0)
    serverSocket = new ServerSocket(0, 1);
    // just get the allocated port
    port = serverSocket.getLocalPort();
    // fill backlog queue by this request so consequent requests will be blocked
    socket = new Socket();
    socket.connect(serverSocket.getLocalSocketAddress());
  }

  @AfterEach
  public void after() throws IOException {
    // some cleanup
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
  }

  // Works on macOS (kernel drops SYNs beyond backlog so connect times out) but hangs on
  // Linux CI: kernel accepts connections beyond ServerSocket(0, 1) backlog, so connect
  // succeeds and .block() waits forever for an HTTP response that never comes (clients
  // only configure a connect timeout here, no response timeout).
  @Disabled
  @Test
  public void shouldFailOnConnectionTimeout() {

    Throwable exception = assertThrows(Exception.class, () -> {

      IcecreamServiceApi client = builder(300)
              .target(IcecreamServiceApi.class, "http://localhost:" + port);

      client.findOrder(1).subscribeOn(testScheduler()).block();
    });

    // each backend wraps the connect timeout differently (Apache, Netty, Jetty, Java11 HttpClient,
    // WebClient), so assert the cause chain contains some form of IOException — the common
    // supertype of all real connect/socket timeout exceptions
    assertThat(causeChainContainsIOException(exception))
            .as("expected IOException in cause chain but got: %s", exception)
            .isTrue();
  }

  private static boolean causeChainContainsIOException(Throwable t) {
    while (t != null) {
      if (t instanceof IOException) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

}
