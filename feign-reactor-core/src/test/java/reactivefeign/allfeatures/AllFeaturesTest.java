/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.allfeatures;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import reactivefeign.BaseReactorTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static reactivefeign.ReactivityTest.CALLS_NUMBER;
import static reactivefeign.ReactivityTest.timeToCompleteReactively;
import static reactivefeign.TestUtils.toLowerCaseKeys;
import static reactivefeign.allfeatures.AllFeaturesFeign.DATE_TIME_FORMAT;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Flux.fromArray;
import static reactor.core.publisher.Mono.fromFuture;
import static reactor.core.publisher.Mono.just;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign in conjunction with WebFlux rest controller.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(
		properties = {"spring.main.web-application-type=reactive"},
		classes = {AllFeaturesController.class, AllFeaturesTest.TestConfiguration.class },
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract public class AllFeaturesTest extends BaseReactorTest {

	@Autowired
	private Environment environment;

	protected List<String> getActiveProfiles() {
		return asList(environment.getActiveProfiles());
	}

	protected AllFeaturesApi client;

	@LocalServerPort
	protected int port;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	abstract protected AllFeaturesApi buildClient(String url);

	protected AllFeaturesApi buildClient(){
		return buildClient("http://localhost:" + port);
	}

	@Before
	public void setUp() {
		client = buildClient();
	}

	@Test
	public void shouldReturnAllPassedParameters() {
		Map<String, String> paramMap = new HashMap<String, String>() {{
				put("paramKey1", "paramValue1");
				put("paramKey2", "paramValue2");
			}};
		Map<String, String> returned = client.mirrorParameters(555,"777", paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsEntry("paramInPath", "555");
		assertThat(returned).containsEntry("paramInUrl", "777");
		assertThat(returned).containsAllEntriesOf(paramMap);
	}

	@Test
	public void shouldReturnEmptyPassedParameters() {
		Map<String, String> paramMap = new HashMap<String, String>() {
			{
				put("paramKey", "");
			}
		};
		Map<String, String> returned = client.mirrorParameters(555,"", paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsEntry("paramKey", "");
		assertThat(returned).containsEntry("paramInUrl", "");
	}

	@Test
	public void shouldReturnAllPassedParametersNew() {

		Map<String, String> paramMap = new HashMap<String, String>() {
			{
				put("paramKey", "paramValue");
			}
		};

		Map<String, String> returned = client.mirrorParametersNew(777, 888L, paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsEntry("paramInUrl", "777");
		assertThat(returned).containsEntry("dynamicParam", "888");
		assertThat(returned).containsAllEntriesOf(paramMap);
	}

	@Test
	public void shouldPassEmptyParameterInUrl() {

		Map<String, String> returned = client.passEmptyParameterInUrl()
				.subscribeOn(testScheduler()).block();

		assertThat(returned).hasSize(2);
		assertThat(returned).containsEntry("manufacturingPlan", "ZPH-V121-00123");
		assertThat(returned).containsEntry("workOrder", "");
	}

	@Test
	public void shouldNotReturnNullPassedParametersNew() {
		Map<String, String> paramMap = new HashMap<String, String>() {
			{
				put("paramKey", "paramValue");
				put("paramKeyNull", null);
			}
		};
		Map<String, String> returned = client.mirrorParametersNew(777,null, paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsEntry("paramInUrl", "777");
		assertThat(returned).containsEntry("paramKey", "paramValue");
		assertThat(returned).doesNotContainKeys("dynamicParam", "paramKeyNull");
	}

	@Test
	public void shouldReturnAllPassedListParametersNew() {

		List<Integer> dynamicListParam = asList(1, 2, 3);
		List<Integer> returned = client.mirrorListParametersNew(dynamicListParam)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsAll(dynamicListParam);
	}

	@Test
	public void shouldReturnAllPassedArrayParametersNew() {

		String[] dynamicArrayParam = new String[]{"1", "2", "3"};
		String[] returned = client.mirrorArrayParametersNew(dynamicArrayParam)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsExactly(dynamicArrayParam);
	}

	@Test
	public void shouldReturnEmptyOnNullPassedListParametersNew() {

		List<Integer> returned = client.mirrorListParametersNew(null)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).isEmpty();
	}

	@Test
	public void shouldReturnAllPassedMapParametersNew() {

		Map<String, List<String>> paramMap = new HashMap<String, List<String>>() {
			{
				put("paramKey", asList("paramValue1", "paramValue2"));
			}
		};

		Map<String, List<String>> returned = client.mirrorMapParametersNew(paramMap)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsAllEntriesOf(paramMap);
	}

	@Test
	public void shouldReturnEmptyOnNullPassedMapParametersNew() {

		Map<String, List<String>> returned = client.mirrorMapParametersNew(null)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).isEmpty();
	}

	@Test
	public void shouldReturnAllPassedHeaders() {
		Map<String, String> headersMap = new HashMap<String, String>() {
			{
				put("headerKey1", "headerValue1");
				put("headerKey2", "headerValue2");
			}
		};
		Map<String, String> returned = toLowerCaseKeys(client.mirrorHeaders(777, headersMap)
				.subscribeOn(testScheduler()).block());

		assertThat(returned).containsEntry("method-header", "777");
		assertThat(returned).containsAllEntriesOf(toLowerCaseKeys(headersMap));
		assertThat(returned).containsKey("accept");
	}

	@Test
	public void shouldReturnAllPassedListHeaders() {
		List<Long> listHeader = asList(111L, 777L);
		List<Long> returned = client.mirrorListHeaders(listHeader)
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsAll(listHeader);
	}

	@Test
	public void shouldReturnAllPassedMultiMapHeaders() {
		Map<String, List<String>> headersMap = new HashMap<String, List<String>>() {
			{
				put("headerKey1", asList("headerValue1", "headerValue2"));
			}
		};
		Map<String, List<String>> returned = client.mirrorMultiMapHeaders(headersMap)
				.subscribeOn(testScheduler()).block();

		assertThat(toLowerCaseKeys(returned)).containsAllEntriesOf(toLowerCaseKeys(headersMap));
	}

	@Test
	public void shouldPassHeaderAndRequestParameterWithSameName() {
		String[] returned = client.mirrorHeaderAndRequestWithSameName("1", "2")
				.subscribeOn(testScheduler()).block();

		assertThat(returned).containsExactly("1", "2");
	}

	@Test
	public void shouldReturnBody() {
		String returned = client.mirrorBody("Test Body")
				.subscribeOn(testScheduler()).block();

		assertThat(returned).isEqualTo("Test Body");
	}

	@Test
	public void shouldReturnBodyMap() {
		Map<String, String> bodyMap = new HashMap<String, String>() {
			{
				put("key1", "value1");
				put("key2", "value2");
			}
		};

		Map<String, String> returned = client.mirrorBodyMap(bodyMap)
				.subscribeOn(testScheduler()).block();
		assertThat(returned).containsAllEntriesOf(bodyMap);
	}

	@Test
	public void shouldReturnBodyReactive() {
		String returned = client.mirrorBodyReactive(just("Test Body"))
				.subscribeOn(testScheduler()).block();
		assertThat(returned).isEqualTo("Test Body");
	}

	@Test
	public void shouldReturnBodyMapReactive() {
		Map<String, String> bodyMap = new HashMap<String, String>() {
			{
				put("key1", "value1");
				put("key2", "value2");
			}
		};

		Mono<Map<String, String>> publisher = client.mirrorBodyMapReactive(just(bodyMap))
				.subscribeOn(testScheduler());
		StepVerifier.create(publisher)
				.consumeNextWith(map -> assertThat(map).containsAllEntriesOf(bodyMap))
				.verifyComplete();
	}

	@Test
	public void shouldReturnFirstResultBeforeSecondSent() throws InterruptedException {

		CountDownLatch countDownLatch = new CountDownLatch(2);

		AtomicInteger sentCount = new AtomicInteger();
		AtomicInteger receivedCount = new AtomicInteger();

		CompletableFuture<AllFeaturesApi.TestObject> firstReceived = new CompletableFuture<>();

		Flux<AllFeaturesApi.TestObject> returned = client
				.mirrorBodyStream(Flux.just(new AllFeaturesApi.TestObject("testMessage1"),
						new AllFeaturesApi.TestObject("testMessage2"))
						.delayUntil(testObject -> sentCount.get() == 1 ? fromFuture(firstReceived)
								: empty())
						.doOnNext(sent -> sentCount.incrementAndGet())
				).subscribeOn(testScheduler());

		returned.doOnNext(received -> {
			receivedCount.incrementAndGet();
			firstReceived.complete(received);
			countDownLatch.countDown();
		}).subscribe();

		countDownLatch.await();
	}

	@Test
	public void shouldReturnEmpty() {
		Optional<AllFeaturesApi.TestObject> returned = client.empty()
				.subscribeOn(testScheduler()).blockOptional();
		assertThat(!returned.isPresent());
	}

	@Test
	public void shouldReturnDefaultBody() {
		String returned = client.mirrorDefaultBody()
				.subscribeOn(testScheduler()).block();
		assertThat(returned).isEqualTo("default");
	}


	@Test
	public void shouldRunReactively() {

		AtomicInteger counter = new AtomicInteger();

		for (int i = 0; i < CALLS_NUMBER; i++) {
			client.mirrorBodyWithDelay("testBody")
					.doOnNext(order -> counter.incrementAndGet())
					.subscribeOn(testScheduler())
					.subscribe();
		}

		waitAtMost(timeToCompleteReactively(), TimeUnit.MILLISECONDS)
				.until(() -> counter.get() == CALLS_NUMBER);
	}

	@Test
	public void shouldMirrorIntegerStreamBody() {
		Flux<Integer> result = client.mirrorIntegerBodyStream(fromArray(new Integer[]{1, 3, 5, 7}))
				.subscribeOn(testScheduler());

		StepVerifier.create(result)
				.expectNext(1)
				.expectNext(3)
				.expectNext(5)
				.expectNext(7)
				.verifyComplete();
	}

	@Test
	public void shouldMirrorStringStreamBody() {
		Flux<String> result = client.mirrorStringBodyStream(fromArray(new String[]{"a", "b", "c"}))
				.subscribeOn(testScheduler());

		StepVerifier.create(result)
				.expectNext("a")
				.expectNext("b")
				.expectNext("c")
				.verifyComplete();
	}

	@Test
	public void shouldMirrorBinaryBody() {
		StepVerifier.create(client.mirrorStreamingBinaryBodyReactive(
				Mono.just(fromByteArray(new byte[]{1,2,3})))
				.subscribeOn(testScheduler()))
				.consumeNextWith(buffer -> {
					byte[] dataReceived = new byte[buffer.limit()];
					buffer.get(dataReceived);
					assertThat(dataReceived).isEqualTo(new byte[]{1,2,3});
				})
				.verifyComplete();
	}

	@Test
	public void shouldMirrorStreamingBinaryBodyReactive() throws InterruptedException {

		CountDownLatch countDownLatch = new CountDownLatch(2);

		AtomicInteger sentCount = new AtomicInteger();
		ConcurrentLinkedQueue<byte[]> receivedAll = new ConcurrentLinkedQueue<>();

		CompletableFuture<ByteBuffer> firstReceived = new CompletableFuture<>();

		Flux<ByteBuffer> returned = client.mirrorStreamingBinaryBodyReactive(
				Flux.just(fromByteArray(new byte[]{1,2,3}), fromByteArray(new byte[]{4,5,6})))
				.subscribeOn(testScheduler())
				.delayUntil(testObject -> sentCount.get() == 1 ? fromFuture(firstReceived)
						: empty())
				.doOnNext(sent -> sentCount.incrementAndGet());

		returned.doOnNext(received -> {
			byte[] dataReceived = new byte[received.limit()];
			received.get(dataReceived);
			receivedAll.add(dataReceived);
			assertThat(receivedAll.size()).isEqualTo(sentCount.get());
			firstReceived.complete(received);
			countDownLatch.countDown();
		}).subscribe();

		countDownLatch.await();

		assertThat(receivedAll).containsExactly(new byte[]{1,2,3}, new byte[]{4,5,6});
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfNoSubstitutionForPath(){
		client.urlNotSubstituted()
				.subscribeOn(testScheduler()).block();
	}


	@Test
	public void shouldEncodeQueryParam() {
		String QUERY_PARAM_VALUE = "query param value with space and Cyrillic Героям Слава";

		StepVerifier.create(client.encodeParam(QUERY_PARAM_VALUE)
				.subscribeOn(testScheduler()))
				.expectNextMatches(testObject -> testObject.payload.equals(QUERY_PARAM_VALUE))
				.verifyComplete();
	}

	@Test
	public void shouldEncodeQueryParamWithReservedChars() {
		String QUERY_PARAM_VALUE = "workers?in=(\"123/321\")";

		StepVerifier.create(client.encodeParam(QUERY_PARAM_VALUE)
				.subscribeOn(testScheduler()))
				.expectNextMatches(testObject -> testObject.payload.equals(QUERY_PARAM_VALUE))
				.verifyComplete();
	}

	@Test
	public void shouldEncodePathParam() {
		String PATH_PARAM = "path value with space and Cyrillic Героям Слава";

		StepVerifier.create(client.encodePath(PATH_PARAM)
				.subscribeOn(testScheduler()))
				.expectNextMatches(testObject -> testObject.payload.equals(PATH_PARAM))
				.verifyComplete();

	}

	@Test
	public void shouldEncodePathParamWithReservedChars() {
		String PATH_PARAM = "workers?in=(\"123/321\")";

		StepVerifier.create(client.encodePath(PATH_PARAM)
				.subscribeOn(testScheduler()))
				.expectNextMatches(testObject -> testObject.payload.equals(PATH_PARAM))
				.verifyComplete();

	}

	@Test
	public void shouldExpandPathParam() {

		int timestamp = 1234;
		StepVerifier.create(client.expandPathParameter(timestamp)
				.subscribeOn(testScheduler()))
				.expectNextMatches(result -> result.payload.equals(new Date(timestamp).toString()))
				.verifyComplete();
	}

	@Test
	public void shouldExpandRequestParamWithCustomFormat() {

		LocalDateTime dateTime = LocalDateTime.of(2021, Month.DECEMBER, 1, 1, 1);
		StepVerifier.create(client.expandDataTimeParameterWithCustomFormat(dateTime)
				.subscribeOn(testScheduler()))
				.expectNextMatches(result -> result.payload.equals(dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
				.verifyComplete();
	}

	private static ByteBuffer fromByteArray(byte[] data){
		return ByteBuffer.wrap(data);
	}

	@Configuration
	@Profile("netty")
	public static class TestConfiguration{

		@Bean
		public ReactiveWebServerFactory reactiveWebServerFactory(){
			return new NettyReactiveWebServerFactory();
		}
	}

}
