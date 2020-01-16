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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.client.log.DefaultReactiveLogger;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * @author Sergii Karpenko
 */
@NotThreadSafe
abstract public class LoggerTest {

  private static final String LOGGER_NAME = DefaultReactiveLogger.class.getName();

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig()
          .asynchronousResponseEnabled(true)
          .dynamicPort());

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder();

  abstract protected ReactiveFeignBuilder<IcecreamServiceApi> builder(long readTimeoutInMillis);

  abstract protected String appenderPrefix();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Test
  public void shouldLogMono() throws Exception {

    Appender appender = createAppender("TestMonoAppender");

    Level originalLevel = setLogLevel(Level.TRACE);

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
        .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

    IcecreamServiceApi client = builder()
        .target(IcecreamServiceApi.class,
            "http://localhost:" + wireMockRule.port());

    Mono<Bill> billMono = client.makeOrder(order);

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor);

    billMono.block();

    Mockito.verify(appender, times(7)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    assertLogEvent(logEvents, 0, Level.DEBUG,
        "[IcecreamServiceApi#makeOrder]--->POST http://localhost");
    assertLogEvent(logEvents, 1, Level.TRACE,
        "[IcecreamServiceApi#makeOrder] REQUEST HEADERS\n" +
            "Accept:[application/json]");
    assertLogEvent(logEvents, 2, Level.TRACE,
        "[IcecreamServiceApi#makeOrder] REQUEST BODY\n" +
            "IceCreamOrder{ id=20, balls=");
    assertLogEvent(logEvents, 3, Level.TRACE,
        "[IcecreamServiceApi#makeOrder] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, 4, Level.DEBUG,
        "[IcecreamServiceApi#makeOrder]<--- headers takes");
    assertLogEvent(logEvents, 5, Level.TRACE,
        "[IcecreamServiceApi#makeOrder] RESPONSE BODY\n" +
            "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, 6, Level.DEBUG,
        "[IcecreamServiceApi#makeOrder]<--- body takes");

    setLogLevel(originalLevel);
    removeAppender(appender.getName());
  }

  @Test
  public void shouldLogFlux() throws Exception {

    Appender appender = createAppender("TestFluxAppender");

    Level originalLevel = setLogLevel(Level.TRACE);

    IceCreamOrder order1 = new OrderGenerator().generate(21);
    Bill billExpected1 = Bill.makeBill(order1);

    IceCreamOrder order2 = new OrderGenerator().generate(22);
    Bill billExpected2 = Bill.makeBill(order2);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders/batch"))
            .withRequestBody(equalTo(fluxRequestBody(asList(order1, order2))))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.MAPPER.writeValueAsString(asList(billExpected1, billExpected2)))));

    IcecreamServiceApi client = builder()
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    Flux<Bill> billsFlux = client.makeOrders(Flux.just(order1, order2));

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor);

    billsFlux.collectList().block();

    Mockito.verify(appender, times(10)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    assertLogEvent(logEvents, 0, Level.DEBUG,
            "[IcecreamServiceApi#makeOrders]--->POST http://localhost");
    assertLogEvent(logEvents, 1, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] REQUEST HEADERS\n" +
                    "Accept:[application/json]");
    assertLogEvent(logEvents, 2, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] REQUEST BODY ELEMENT\n" +
                    "IceCreamOrder{ id=21, balls=");
    assertLogEvent(logEvents, 3, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] REQUEST BODY ELEMENT\n" +
                    "IceCreamOrder{ id=22, balls=");
    assertLogEvent(logEvents, 4, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, 5, Level.DEBUG,
            "[IcecreamServiceApi#makeOrders]<--- headers takes");
    assertLogEvent(logEvents, 6, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] RESPONSE BODY ELEMENT\n" +
                    "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, 7, Level.DEBUG,
            "[IcecreamServiceApi#makeOrders]<--- body takes");
    assertLogEvent(logEvents, 8, Level.TRACE,
            "[IcecreamServiceApi#makeOrders] RESPONSE BODY ELEMENT\n" +
                    "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, 9, Level.DEBUG,
            "[IcecreamServiceApi#makeOrders]<--- body takes");

    setLogLevel(originalLevel);
    removeAppender(appender.getName());
  }

  protected String fluxRequestBody(List<?> list) throws JsonProcessingException {
    return TestUtils.MAPPER.writeValueAsString(list);
  }

  @Test
  public void shouldLogNoBody() {

    Appender appender = createAppender("TestPingAppender");

    Level originalLevel = setLogLevel(Level.TRACE);

    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    IcecreamServiceApi client = builder()
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    Mono<Void> ping = client.ping();

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor);

    ping.block();

    Mockito.verify(appender, times(4)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    assertLogEvent(logEvents, 0, Level.DEBUG,
            "[IcecreamServiceApi#ping]--->GET http://localhost");
    assertLogEvent(logEvents, 1, Level.TRACE,
            "[IcecreamServiceApi#ping] REQUEST HEADERS\n" +
                    "Accept:[application/json]");
    assertLogEvent(logEvents, 2, Level.TRACE,
            "[IcecreamServiceApi#ping] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, 3, Level.DEBUG,
            "[IcecreamServiceApi#ping]<--- headers takes");

    setLogLevel(originalLevel);
    removeAppender(appender.getName());
  }

  @Test(expected = ReadTimeoutException.class)
  public void shouldLogTimeout() {

    Appender appender = createAppender("TestTimeoutAppender");

    Level originalLevel = setLogLevel(Level.TRACE);

    int readTimeoutInMillis = 100;
    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse()
                    .withFixedDelay(readTimeoutInMillis * 2)
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

    IcecreamServiceApi client = builder(readTimeoutInMillis)
            .target(IcecreamServiceApi.class,
                    "http://localhost:" + wireMockRule.port());

    Mono<Void> ping = client.ping();

    assertNoEventsBeforeSubscription(appender, argumentCaptor);

    try {
      ping.block();
      fail("should throw ReadTimeoutException");
    }
    catch (ReadTimeoutException e) {
      Mockito.verify(appender, times(3)).append(argumentCaptor.capture());

      List<LogEvent> logEvents = argumentCaptor.getAllValues();
      assertLogEvent(logEvents, 0, Level.DEBUG,
              "[IcecreamServiceApi#ping]--->GET http://localhost");
      assertLogEvent(logEvents, 1, Level.TRACE,
              "[IcecreamServiceApi#ping] REQUEST HEADERS\n" +
                      "Accept:[application/json]");
      assertLogEvent(logEvents, 2, Level.ERROR,
              "[IcecreamServiceApi#ping]--->GET http://localhost");

      throw e;
    }
    finally {
      setLogLevel(originalLevel);
      removeAppender(appender.getName());
    }
  }

  private void assertNoEventsBeforeSubscription(Appender appender, ArgumentCaptor<LogEvent> argumentCaptor) {
    Mockito.verify(appender, atLeast(0)).append(argumentCaptor.capture());
    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    assertThat(logEvents).isEmpty();
  }

  private void assertLogEvent(List<LogEvent> events, int index, Level level, String message) {
    assertThat(events).element(index)
        .hasFieldOrPropertyWithValue("level", level)
        .extracting("message")
        .extracting("getFormattedMessage")
        .has(new Condition<>(o -> ((String) o).contains(message), "check message"));
  }

  private void assertLogEvent(List<LogEvent> events, int index, Level level, String message1, String message2) {
    assertThat(events).element(index)
            .hasFieldOrPropertyWithValue("level", level)
            .extracting("message")
            .extracting("getFormattedMessage")
            .has(new Condition<>(o -> ((String) o).toLowerCase().contains(message1.toLowerCase()), "check message1"))
            .has(new Condition<>(o -> ((String) o).toLowerCase().contains(message2.toLowerCase()), "check message2"));;
  }

  public Appender createAppender(String name) {
    Appender appender = Mockito.mock(Appender.class);
    when(appender.getName()).thenReturn(appenderPrefix()+name);
    when(appender.isStarted()).thenReturn(true);
    getLoggerConfig().addAppender(appender, Level.ALL, null);
    return appender;
  }

  public void removeAppender(String name) {
    getLoggerConfig().removeAppender(name);
  }

  private static Level setLogLevel(Level logLevel) {
    LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    Configuration configuration = loggerContext.getConfiguration();
    LoggerConfig loggerConfig = configuration.getLoggerConfig(LOGGER_NAME);
    Level previousLevel = loggerConfig.getLevel();
    loggerConfig.setLevel(logLevel);
    loggerContext.updateLoggers();
    return previousLevel;
  }

  private static LoggerConfig getLoggerConfig() {
    LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    Configuration configuration = loggerContext.getConfiguration();
    configuration.addLogger(LOGGER_NAME, new LoggerConfig());
    return configuration.getLoggerConfig(LOGGER_NAME);
  }
}
