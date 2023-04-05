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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AllFeaturesApi {

	Mono<Map<String, String>> mirrorParameters(
            long paramInPath,
            String paramInQuery,
            Map<String, String> paramMap);

	Mono<Map<String, String>> mirrorParametersNew(
            long paramInUrl,
            Long dynamicParam,
            Map<String, String> paramMap);

	Mono<Map<String, String>> passEmptyParameterInUrl();

	Mono<List<Integer>> mirrorListParametersNew(
			List<Integer> dynamicListParam);

	Mono<String[]> mirrorArrayParametersNew(
			String[] dynamicArrayParam);

	Mono<Map<String, List<String>>> mirrorMapParametersNew(
			Map<String, List<String>> paramMap);

	Mono<Map<String, String>> mirrorHeaders(long param,
                                            Map<String, String> paramMap);

	Mono<List<Long>> mirrorListHeaders(
			List<Long> param);

	Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
			Map<String, List<String>> headerMap);

	Mono<String[]> mirrorHeaderAndRequestWithSameName(String header, String requestParam);

	Mono<String> mirrorBody(String body);

	Mono<Map<String, String>> mirrorBodyMap(Map<String, String> body);

	Mono<String> mirrorBodyReactive(Publisher<String> body);

	Mono<Map<String, String>> mirrorBodyMapReactive(Publisher<Map<String, String>> body);

	Flux<TestObject> mirrorBodyStream(Publisher<TestObject> bodyStream);

	Flux<Integer> mirrorIntegerBodyStream(Flux<Integer> body);

	Flux<String> mirrorStringBodyStream(Flux<String> body);

	Mono<TestObject> empty();

	Mono<String> mirrorBodyWithDelay(String body);

    Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(Publisher<ByteBuffer> body);

	Mono<String> urlNotSubstituted();

	default Mono<String> mirrorDefaultBody() {
		return mirrorBody("default");
	}

	Mono<TestObject> encodeParam(String param);

	Mono<TestObject> encodePath(String param);

	Mono<TestObject> expandPathParameter(long timestamp);

	Mono<TestObject> expandPathParameterInRequestParameter(String companyName);

	Mono<TestObject> expandDataTimeParameterWithCustomFormat(LocalDateTime dateTime);

	Mono<TestObject> formDataMap(Map<String, ?> form);

	Mono<TestObject> formDataParameters(
			String organizationName,
			String organizationId);

	class TestObject {

		public String payload;

		public TestObject() {
		}

		public TestObject(String payload) {
			this.payload = payload;
		}
	}

}
