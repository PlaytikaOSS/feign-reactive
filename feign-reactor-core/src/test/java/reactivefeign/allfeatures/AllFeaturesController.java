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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static reactor.core.publisher.Mono.just;

@RestController
public class AllFeaturesController implements AllFeaturesApi {

	@GetMapping(path = "/mirrorParameters/{paramInPath}")
	@Override
	public Mono<Map<String, String>> mirrorParameters(
			@PathVariable("paramInPath") long paramInPath,
			@RequestParam("paramInUrl") String paramInUrl,
			@RequestParam Map<String, String> paramMap) {
		Map<String, String> resultMap = paramMap != null
				? new HashMap<>(paramMap) : new HashMap<>();
		resultMap.put("paramInPath", Long.toString(paramInPath));
		resultMap.put("paramInUrl", paramInUrl);
		return just(resultMap);
	}

	@GetMapping(path = "/mirrorParametersNew")
	@Override
	public Mono<Map<String, String>> mirrorParametersNew(
			@RequestParam("paramInUrl") long paramInUrl,
			@RequestParam(value = "dynamicParam", required = false) Long dynamicParam,
			@RequestParam Map<String, String> paramMap) {
		paramMap.put("paramInUrl", Long.toString(paramInUrl));
		if(dynamicParam != null) {
			paramMap.put("dynamicParam", Long.toString(dynamicParam));
		}
		return just(paramMap);
	}

	@GetMapping(path = "/mirrorListParametersNew")
	@Override
	public Mono<List<Integer>> mirrorListParametersNew(
			@RequestParam(value = "dynamicListParam", required = false) List<Integer> listParams) {
		return listParams != null ? just(listParams) : Mono.just(emptyList());
	}

	//Spring can't treat Map<String, List<String>> correctly
	@Override
	public Mono<Map<String, List<String>>> mirrorMapParametersNew(
			Map<String, List<String>> paramMap) {
		throw new UnsupportedOperationException();
	}

	@GetMapping(path = "/mirrorMapParametersNew")
	public Mono<Map<String, List<String>>> mirrorMapParametersNew(
			//Spring can't treat Map<String, List<String>> correctly
			@RequestParam MultiValueMap<String, String> paramMap) {
		return just(paramMap);
	}

	@GetMapping(path = "/mirrorHeaders")
	@Override
	public Mono<Map<String, String>> mirrorHeaders(
			@RequestHeader("Method-Header") long param,
			@RequestHeader Map<String, String> headersMap) {
		return just(headersMap);
	}

	@GetMapping(path = "/mirrorListHeaders")
	@Override
	public Mono<List<Long>> mirrorListHeaders(
			@RequestHeader("Method-Header") List<Long> param) {
		return just(param);
	}

	//Spring can't treat Map<String, List<String>> correctly
	@Override
	public Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
			Map<String, List<String>> param) {
		throw new UnsupportedOperationException();
	}

	@GetMapping(path = "/mirrorMultiMapHeaders")
	public Mono<MultiValueMap<String, String>> mirrorMultiMapHeaders(
			//Spring can't treat Map<String, List<String>> correctly
			@RequestHeader MultiValueMap<String, String> param) {
		return just(param);
	}

	@PostMapping(path = "/mirrorBody")
	@Override
	public Mono<String> mirrorBody(@RequestBody String body) {
		return just(body);
	}

	@PostMapping(path = "/mirrorBodyMap")
	@Override
	public Mono<Map<String, String>> mirrorBodyMap(
			@RequestBody Map<String, String> body) {
		return just(body);
	}

	@PostMapping(path = "/mirrorBodyReactive")
	@Override
	public Mono<String> mirrorBodyReactive(@RequestBody Publisher<String> body) {
		return Mono.from(body);
	}

	@PostMapping(path = "/mirrorBodyMapReactive")
	@Override
	public Mono<Map<String, String>> mirrorBodyMapReactive(
			@RequestBody Publisher<Map<String, String>> body) {
		return Mono.from(body);
	}

	@PostMapping(path = "/mirrorBodyStream")
	@Override
	public Flux<AllFeaturesApi.TestObject> mirrorBodyStream(
			@RequestBody Publisher<AllFeaturesApi.TestObject> bodyStream) {
		return Flux.from(bodyStream);
	}

	@PostMapping(path = "/mirrorIntegerBodyStream")
	@Override
	public Flux<Integer> mirrorIntegerBodyStream(
			@RequestBody Flux<Integer> body){
		return body;
	}

	@PostMapping(path = "/mirrorStringBodyStream")
	@Override
	public Flux<String> mirrorStringBodyStream(
			@RequestBody  Flux<String> body){
		return body;
	}

	@PostMapping(path = "/mirrorBodyWithDelay")
	@Override
	public Mono<String> mirrorBodyWithDelay(@RequestBody String body) {
		return just(body).delayElement(Duration.ofMillis(500));
	}

	@Override
	public Mono<TestObject> empty() {
		return Mono.empty();
	}

	@PostMapping(path = "/mirrorStreamingBinaryBodyReactive")
	@Override
	public Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(@RequestBody Publisher<ByteBuffer> body) {
		return Flux.from(body);
	}

	@GetMapping(path = "/urlNotSubstituted")
	@Override
	public Mono<String> urlNotSubstituted(){
		throw new UnsupportedOperationException("should be never called as contain not substituted element in path");
	}

}
