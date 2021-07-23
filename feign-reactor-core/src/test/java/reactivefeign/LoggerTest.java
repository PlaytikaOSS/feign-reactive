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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactivefeign.client.ReadTimeoutException;
import reactivefeign.client.log.DefaultReactiveLogger;
import reactivefeign.publisher.retry.RetryPublisherHttpClient;
import reactivefeign.retry.BasicReactiveRetryPolicy;
import reactivefeign.testcase.IcecreamServiceApi;
import reactivefeign.testcase.domain.Bill;
import reactivefeign.testcase.domain.IceCreamOrder;
import reactivefeign.testcase.domain.OrderGenerator;
import reactivefeign.utils.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static reactivefeign.RetryingTest.mockResponseAfterSeveralAttempts;
import static reactivefeign.client.ReactiveHttpRequestInterceptors.addHeaders;
import static reactivefeign.utils.HttpStatus.SC_SERVICE_UNAVAILABLE;

/**
 * @author Sergii Karpenko
 */
abstract public class LoggerTest<T extends IcecreamServiceApi> extends BaseReactorTest {

  private static final String[] LOGGER_NAMES = {
          DefaultReactiveLogger.class.getName(),
          RetryPublisherHttpClient.class.getName()
  };

  private static final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);

  @Rule
  public WireMockClassRule wireMockRule = new WireMockClassRule(
      wireMockConfig()
          .asynchronousResponseEnabled(true)
          .dynamicPort());

  abstract protected ReactiveFeignBuilder<T> builder();

  abstract protected Class<T> target();

  abstract protected ReactiveFeignBuilder<T> builder(long readTimeoutInMillis);

  abstract protected String appenderPrefix();

  protected WireMockConfiguration wireMockConfig(){
    return WireMockConfiguration.wireMockConfig();
  }

  @Before
  public void resetServers() {
    wireMockRule.resetAll();
  }

  @Test
  public void shouldLogMono() throws Exception {

    Appender appender = createAppender("TestMonoAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
        .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
        .willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

    T client = builder()
        .target(target(),
            "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Mono<Bill> billMono = client.makeOrder(order).subscribeOn(testScheduler());

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    billMono.block();

    Mockito.verify(appender, atLeast(7)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    AtomicInteger index = new AtomicInteger();
    assertLogEvent(logEvents, index, Level.DEBUG,
        "["+clientName+"#makeOrder(IceCreamOrder)]--->POST http://localhost");
    assertLogEvent(logEvents, index, Level.TRACE,
        "["+clientName+"#makeOrder(IceCreamOrder)] REQUEST HEADERS\n" +
            "Accept:[application/json]");
    assertLogEvent(logEvents, index, Level.TRACE,
        "["+clientName+"#makeOrder(IceCreamOrder)] REQUEST BODY\n" +
            "IceCreamOrder{ id=20, balls=");
    assertLogEvent(logEvents, index, Level.TRACE,
        "["+clientName+"#makeOrder(IceCreamOrder)] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, index, Level.DEBUG,
        "["+clientName+"#makeOrder(IceCreamOrder)]<--- headers takes");
    assertLogEvent(logEvents, index, Level.TRACE,
        "["+clientName+"#makeOrder(IceCreamOrder)] RESPONSE BODY\n" +
            "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, index, Level.DEBUG,
        "["+clientName+"#makeOrder(IceCreamOrder)]<--- body takes");

    rollbackLogLevels(originalLevels);
    removeAppender(appender.getName());
  }

  @Test
  public void shouldLogFlux() throws Exception {

    Appender appender = createAppender("TestFluxAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    IceCreamOrder order1 = new OrderGenerator().generate(21);
    Bill billExpected1 = Bill.makeBill(order1);

    IceCreamOrder order2 = new OrderGenerator().generate(22);
    Bill billExpected2 = Bill.makeBill(order2);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders/batch"))
            .withRequestBody(equalTo(fluxRequestBody(asList(order1, order2))))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.MAPPER.writeValueAsString(asList(billExpected1, billExpected2)))));

    T client = builder()
            .target(target(),
                    "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Flux<Bill> billsFlux = client.makeOrders(Flux.just(order1, order2)).subscribeOn(testScheduler());

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    billsFlux.collectList().block();

    Mockito.verify(appender, atLeast(10)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    AtomicInteger index = new AtomicInteger();
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrders(Flux)]--->POST http://localhost");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] REQUEST HEADERS\n" +
                    "Accept:[application/json]");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] REQUEST BODY ELEMENT\n" +
                    "IceCreamOrder{ id=21, balls=");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] REQUEST BODY ELEMENT\n" +
                    "IceCreamOrder{ id=22, balls=");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrders(Flux)]<--- headers takes");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] RESPONSE BODY ELEMENT\n" +
                    "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrders(Flux)]<--- body takes");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrders(Flux)] RESPONSE BODY ELEMENT\n" +
                    "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrders(Flux)]<--- body takes");

    rollbackLogLevels(originalLevels);
    removeAppender(appender.getName());
  }

  protected String fluxRequestBody(List<?> list) throws JsonProcessingException {
    return TestUtils.MAPPER.writeValueAsString(list);
  }

  @Test
  public void shouldLogNoBody() {

    Appender appender = createAppender("TestPingAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    T client = builder()
            .target(target(),
                    "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Mono<Void> ping = client.ping();

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    ping.subscribeOn(testScheduler()).block();

    Mockito.verify(appender, atLeast(4)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    AtomicInteger index = new AtomicInteger();
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#ping()]--->GET http://localhost");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#ping()] REQUEST HEADERS\n" +
                    "Accept:[application/json]");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#ping()] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#ping()]<--- headers takes");

    rollbackLogLevels(originalLevels);
    removeAppender(appender.getName());
  }

  @Test(expected = ReadTimeoutException.class)
  public void shouldLogTimeout() {

    Appender appender = createAppender("TestTimeoutAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    int readTimeoutInMillis = 100;
    wireMockRule.stubFor(get(urlEqualTo("/ping"))
            .willReturn(aResponse()
                    .withFixedDelay(readTimeoutInMillis * 2)
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")));

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

    T client = builder(readTimeoutInMillis)
            .target(target(), "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Mono<Void> ping = client.ping().subscribeOn(testScheduler());

    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    try {
      ping.block();

      fail("should throw ReadTimeoutException");
    }
    catch (ReadTimeoutException e) {
      Mockito.verify(appender, atLeast(3)).append(argumentCaptor.capture());

      List<LogEvent> logEvents = argumentCaptor.getAllValues();
      AtomicInteger index = new AtomicInteger();
      assertLogEvent(logEvents, index, Level.DEBUG,
              "["+clientName+"#ping()]--->GET http://localhost");
      assertLogEvent(logEvents, index, Level.TRACE,
              "["+clientName+"#ping()] REQUEST HEADERS\n" +
                      "Accept:[application/json]");
      assertLogEvent(logEvents, index, Level.ERROR,
              "["+clientName+"#ping()]--->GET http://localhost");

      throw e;
    }
    finally {
      rollbackLogLevels(originalLevels);
      removeAppender(appender.getName());
    }
  }

  @Test
  public void shouldLogRequestInterceptor() throws Exception {

    Appender appender = createAppender("TestRequestInterceptorAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    IceCreamOrder order = new OrderGenerator().generate(20);
    Bill billExpected = Bill.makeBill(order);

    wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
            .withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
            .willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

    T client = builder()
            .addRequestInterceptor(addHeaders(singletonList(new Pair<>("Authorization", "Bearer mytoken123"))))
            .target(target(),
                    "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Mono<Bill> billMono = client.makeOrder(order).subscribeOn(testScheduler());

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    billMono.block();

    Mockito.verify(appender, atLeast(7)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    AtomicInteger index = new AtomicInteger();
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrder(IceCreamOrder)]--->POST http://localhost");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrder(IceCreamOrder)] REQUEST HEADERS\n" +
                    "Accept:[application/json]\n" +
                    "Content-Type:[application/json]\n" +
                    "Authorization:[Bearer mytoken123]");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrder(IceCreamOrder)] REQUEST BODY\n" +
                    "IceCreamOrder{ id=20, balls=");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrder(IceCreamOrder)] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrder(IceCreamOrder)]<--- headers takes");
    assertLogEvent(logEvents, index, Level.TRACE,
            "["+clientName+"#makeOrder(IceCreamOrder)] RESPONSE BODY\n" +
                    "reactivefeign.testcase.domain.Bill");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "["+clientName+"#makeOrder(IceCreamOrder)]<--- body takes");

    rollbackLogLevels(originalLevels);
    removeAppender(appender.getName());
  }

  @Test
  public void shouldLogRetryMono() throws Exception {

    Appender appender = createAppender("TestMonoRetryAppender");

    Map<LoggerConfig, Level> originalLevels = setLogLevel(Level.TRACE);

    String orderStr = TestUtils.MAPPER.writeValueAsString(new OrderGenerator().generate(1));
    mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
            "/icecream/orders/1",
            aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER, "1"),
            aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                    .withBody(orderStr));

    int maxRetries = 3;
    T client = builder()
            .retryWhen(BasicReactiveRetryPolicy.retryWithBackoff(maxRetries, 0))
            .target(target(),
                    "http://localhost:" + wireMockRule.port());
    String clientName = target().getSimpleName();

    Mono<IceCreamOrder> order = client.findOrder(1).subscribeOn(testScheduler());

    ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
    assertNoEventsBeforeSubscription(appender, argumentCaptor, clientName);

    order.block();

    Mockito.verify(appender, atLeast(7)).append(argumentCaptor.capture());

    List<LogEvent> logEvents = argumentCaptor.getAllValues();
    AtomicInteger index = new AtomicInteger();
    for(int i = 0; i < maxRetries - 1; i++) {
      assertLogEvent(logEvents, index, Level.DEBUG,
              "[" + clientName + "#findOrder(int)]--->GET http://localhost");
      assertLogEvent(logEvents, index, Level.TRACE,
              "[" + clientName + "#findOrder(int)] REQUEST HEADERS\n" +
                      "Accept:[application/json]");
      assertLogEvent(logEvents, index, Level.TRACE,
              "[" + clientName + "#findOrder(int)] RESPONSE HEADERS",
              "Retry-After:1");
      assertLogEvent(logEvents, index, Level.DEBUG,
              "[" + clientName + "#findOrder(int)]<--- headers takes");
      assertLogEvent(logEvents, index, Level.TRACE,
              "[" + clientName + "#findOrder(int)] RESPONSE BODY\n" +
                      "[]");
      assertLogEvent(logEvents, index, Level.DEBUG,
              "[" + clientName + "#findOrder(int)]<--- body takes");
      assertLogEvent(logEvents, index, Level.DEBUG,
              "[" + clientName + "#findOrder(int)]---> RETRYING on error");
    }

    assertLogEvent(logEvents, index, Level.DEBUG,
            "[" + clientName + "#findOrder(int)]--->GET http://localhost");
    assertLogEvent(logEvents, index, Level.TRACE,
            "[" + clientName + "#findOrder(int)] REQUEST HEADERS\n" +
                    "Accept:[application/json]");
    assertLogEvent(logEvents, index, Level.TRACE,
            "[" + clientName + "#findOrder(int)] RESPONSE HEADERS",
            "Content-Type:application/json");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "[" + clientName + "#findOrder(int)]<--- headers takes");
    assertLogEvent(logEvents, index, Level.TRACE,
            "[" + clientName + "#findOrder(int)] RESPONSE BODY\n" +
                    "IceCreamOrder");
    assertLogEvent(logEvents, index, Level.DEBUG,
            "[" + clientName + "#findOrder(int)]<--- body takes");

    rollbackLogLevels(originalLevels);
    removeAppender(appender.getName());
  }

  private void assertNoEventsBeforeSubscription(Appender appender, ArgumentCaptor<LogEvent> argumentCaptor, String clientName) {
    Mockito.verify(appender, atLeast(0)).append(argumentCaptor.capture());
    List<LogEvent> logEvents = argumentCaptor.getAllValues().stream()
            .filter(logEvent -> logEvent.getMessage().getFormattedMessage().contains(clientName))
            .collect(Collectors.toList());
    assertThat(logEvents).isEmpty();
  }

  private void assertLogEvent(List<LogEvent> events, AtomicInteger index, Level level, String message) {
    Throwable t = null;
    for(int i = index.get(); i < events.size(); i++){
      try {
        assertThat(events).element(i)
                .hasFieldOrPropertyWithValue("level", level)
                .extracting("message")
                .extracting("formattedMessage")
                .has(new Condition<>(o -> ((String) o).contains(message), "check message"));
        index.set(i + 1);
        return;
      } catch (Throwable e) {
        t = e;
      }
    }
    if(t != null) {
      throw new RuntimeException(t);
    }
  }

  private void assertLogEvent(List<LogEvent> events, AtomicInteger index, Level level, String message1, String message2) {
    Throwable t = null;
    for(int i = index.get(); i < events.size(); i++){
      try {
        assertThat(events).element(i)
                .hasFieldOrPropertyWithValue("level", level)
                .extracting("message")
                .extracting("formattedMessage")
                .has(new Condition<>(o -> ((String) o).toLowerCase().contains(message1.toLowerCase()), "check message1"))
                .has(new Condition<>(o -> ((String) o).toLowerCase().contains(message2.toLowerCase()), "check message2"));
        index.set(i + 1);
        return;
      } catch (Throwable e) {
        t = e;
      }
    }
    if(t != null) {
      throw new RuntimeException(t);
    }
  }

  public Appender createAppender(String name) {
    Appender appender = Mockito.mock(Appender.class);
    when(appender.getName()).thenReturn(appenderPrefix()+name);
    when(appender.isStarted()).thenReturn(true);
    getLoggerConfigs().forEach(loggerConfig -> loggerConfig.addAppender(appender, Level.ALL, null));
    return appender;
  }

  public void removeAppender(String name) {
    getLoggerConfigs().forEach(loggerConfig -> loggerConfig.removeAppender(name));
  }

  private static Map<LoggerConfig, Level> setLogLevel(Level logLevel) {
    List<LoggerConfig> loggerConfigs = getLoggerConfigs();
    Map<LoggerConfig, Level> previousLevels = loggerConfigs.stream()
            .collect(Collectors.toMap(
                    loggerConfig -> loggerConfig,
                    LoggerConfig::getLevel));
    loggerConfigs.forEach(loggerConfig -> loggerConfig.setLevel(logLevel));
    loggerContext.updateLoggers();
    return previousLevels;
  }

  private static void rollbackLogLevels(Map<LoggerConfig, Level> previousLevels) {
    previousLevels.forEach(LoggerConfig::setLevel);
  }

  private static List<LoggerConfig> getLoggerConfigs() {
    Configuration configuration = loggerContext.getConfiguration();
    return Arrays.stream(LOGGER_NAMES).map(loggerName -> {
      configuration.addLogger(loggerName, new LoggerConfig());
      return configuration.getLoggerConfig(loggerName);
    }).collect(Collectors.toList());

  }
}
