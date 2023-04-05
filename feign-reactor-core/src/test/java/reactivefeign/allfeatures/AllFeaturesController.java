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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static reactivefeign.ReactivityTest.DELAY_IN_MILLIS;
import static reactivefeign.allfeatures.AllFeaturesApi.TestObject;
import static reactor.core.publisher.Mono.just;

@RestController
public class AllFeaturesController implements AllFeaturesMvc {

	@Override
	public Mono<Map<String, String>> mirrorParameters(
			long paramInPath,
			String paramInUrl,
			Map<String, String> paramMap) {
		Map<String, String> resultMap = paramMap != null
				? new HashMap<>(paramMap) : new HashMap<>();
		resultMap.put("paramInPath", Long.toString(paramInPath));
		resultMap.put("paramInUrl", paramInUrl);
		return just(resultMap);
	}

	@Override
	public Mono<Map<String, String>> mirrorParametersNew(
			long paramInUrl,
			Long dynamicParam,
			Map<String, String> paramMap) {
		paramMap.put("paramInUrl", Long.toString(paramInUrl));
		if(dynamicParam != null) {
			paramMap.put("dynamicParam", Long.toString(dynamicParam));
		}
		return just(paramMap);
	}

	@Override
	public Mono<Map<String, String>> mirrorParametersInUrl(Map<String, String> paramMap) {
		return just(paramMap);
	}

	@Override
	public Mono<List<Integer>> mirrorListParametersNew(List<Integer> listParams) {
		return listParams != null ? just(listParams) : Mono.just(emptyList());
	}

	@Override
	public Mono<String[]> mirrorArrayParametersNew(
			String[] dynamicArrayParam){
		return Mono.just(dynamicArrayParam);
	}

	@Override
	public Mono<Map<String, List<String>>> mirrorMapParametersNew(
			//Spring can't treat Map<String, List<String>> correctly
			MultiValueMap<String, String> paramMap) {
		return just(paramMap);
	}

	@Override
	public Mono<Map<String, String>> mirrorHeaders(
			long param,
			Map<String, String> headersMap) {
		return just(headersMap);
	}

	@Override
	public Mono<List<Long>> mirrorListHeaders(
			List<Long> param) {
		return just(param);
	}

	@Override
	public Mono<String[]> mirrorHeaderAndRequestWithSameName(String header, String requestParam) {
		return just(new String[]{header, requestParam});
	}

	@Override
	public Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
			//Spring can't treat Map<String, List<String>> correctly
			MultiValueMap<String, String> param) {
		return just(param);
	}

	@Override
	public Mono<String> mirrorBody(String body) {
		return just(body);
	}

	@Override
	public Mono<Map<String, String>> mirrorBodyMap(
			Map<String, String> body) {
		return just(body);
	}

	@Override
	public Mono<String> mirrorBodyReactive(Publisher<String> body) {
		return Mono.from(body);
	}

	@Override
	public Mono<Map<String, String>> mirrorBodyMapReactive(
			Publisher<Map<String, String>> body) {
		return Mono.from(body);
	}

	@Override
	public Flux<AllFeaturesApi.TestObject> mirrorBodyStream(
			Publisher<AllFeaturesApi.TestObject> bodyStream) {
		return Flux.from(bodyStream);
	}

	@Override
	public Flux<Integer> mirrorIntegerBodyStream(
			Flux<Integer> body){
		return body;
	}

	@Override
	public Flux<String> mirrorStringBodyStream(
			Flux<String> body){
		return body;
	}

	@Override
	public Mono<String> mirrorBodyWithDelay(String body) {
		return just(body).delayElement(Duration.ofMillis(DELAY_IN_MILLIS));
	}

	@Override
	public Mono<TestObject> empty() {
		return Mono.empty();
	}

	@Override
	public Mono<TestObject> encodeParam(String param) {
		return Mono.just(new TestObject(param));
	}

	@Override
	public Mono<TestObject> encodePath(String param) {
		return Mono.just(new TestObject(param));
	}

	@Override
	public Mono<TestObject> expandPathParameter(String date) {
		return Mono.just(new TestObject(date));
	}

	@Override
	public Mono<TestObject> expandRequestParameter(String date){
		return Mono.just(new TestObject(date));
	}

	@Override
	public Mono<TestObject> expandPathParameterInRequestParameter(String companyName) {
		return Mono.just(new TestObject(companyName));
	}

	@Override
	public Mono<AllFeaturesApi.TestObject> formDataMap(ServerWebExchange serverWebExchange){
		return serverWebExchange.getFormData()
				.map(formData -> new AllFeaturesApi.TestObject(formData.toString()));
	}

	@Override
	public Mono<AllFeaturesApi.TestObject> formDataParameters(ServerWebExchange serverWebExchange){
		return serverWebExchange.getFormData()
				.map(formData -> new AllFeaturesApi.TestObject(formData.toString()));
	}

	@Override
	public Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(Publisher<ByteBuffer> body) {
		return Flux.from(body);
	}

	@Override
	public Mono<String> urlNotSubstituted(){
		return Mono.just("should be never called as contain not substituted element in path");
	}

}
