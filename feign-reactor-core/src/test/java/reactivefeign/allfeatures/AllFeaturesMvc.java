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
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static reactivefeign.allfeatures.AllFeaturesApi.TestObject;

public interface AllFeaturesMvc {

	@GetMapping(path = "/mirrorParameters/{paramInPath}")
	Mono<Map<String, String>> mirrorParameters(
			@PathVariable("paramInPath") long paramInPath,
			@RequestParam("paramInUrl") String paramInUrl,
			@RequestParam Map<String, String> paramMap);

	@GetMapping(path = "/mirrorParametersNew")
	Mono<Map<String, String>> mirrorParametersNew(
			@RequestParam("paramInUrl") long paramInUrl,
			@RequestParam(value = "dynamicParam", required = false) Long dynamicParam,
			@RequestParam Map<String, String> paramMap);

	@GetMapping(path = "/mirrorParametersInUrl")
	Mono<Map<String, String>> mirrorParametersInUrl(@RequestParam Map<String, String> paramMap);

	@GetMapping(path = "/mirrorListParametersNew")
	Mono<List<Integer>> mirrorListParametersNew(
			@RequestParam(value = "dynamicListParam", required = false) List<Integer> listParams);

	@GetMapping(path = "/mirrorArrayParametersNew")
	Mono<String[]> mirrorArrayParametersNew(
			@RequestParam("dynamicArrayParam") String[] dynamicArrayParam);

	@GetMapping(path = "/mirrorMapParametersNew")
	Mono<Map<String, List<String>>> mirrorMapParametersNew(
			//Spring can't treat Map<String, List<String>> correctly
			@RequestParam MultiValueMap<String, String> paramMap);

	@GetMapping(path = "/mirrorHeaders")
	Mono<Map<String, String>> mirrorHeaders(
			@RequestHeader("Method-Header") long param,
			@RequestHeader Map<String, String> headersMap);

	@GetMapping(path = "/mirrorListHeaders")
	Mono<List<Long>> mirrorListHeaders(
			@RequestHeader("Method-Header") List<Long> param);

	@GetMapping(path = "/mirrorMultiMapHeaders")
	Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
			//Spring can't treat Map<String, List<String>> correctly
			@RequestHeader MultiValueMap<String, String> param);

	@GetMapping(path = "/mirrorHeaderAndRequestWithSameName")
	Mono<String[]> mirrorHeaderAndRequestWithSameName(
			@RequestHeader("username") String header,
			@RequestParam("username") String requestParam);

	@PostMapping(path = "/mirrorBody")
	Mono<String> mirrorBody(@RequestBody String body);

	@PostMapping(path = "/mirrorBodyMap")
	Mono<Map<String, String>> mirrorBodyMap(
			@RequestBody Map<String, String> body);

	@PostMapping(path = "/mirrorBodyReactive")
	Mono<String> mirrorBodyReactive(@RequestBody Publisher<String> body);

	@PostMapping(path = "/mirrorBodyMapReactive")
	Mono<Map<String, String>> mirrorBodyMapReactive(
			@RequestBody Publisher<Map<String, String>> body);

	@PostMapping(path = "/mirrorBodyStream")
	Flux<TestObject> mirrorBodyStream(
			@RequestBody Publisher<TestObject> bodyStream);

	@PostMapping(path = "/mirrorIntegerBodyStream")
	Flux<Integer> mirrorIntegerBodyStream(
			@RequestBody Flux<Integer> body);

	@PostMapping(path = "/mirrorStringBodyStream")
	Flux<String> mirrorStringBodyStream(
			@RequestBody  Flux<String> body);

	@PostMapping(path = "/mirrorBodyWithDelay")
	Mono<String> mirrorBodyWithDelay(@RequestBody String body);

	@PostMapping(path = "/mirrorStreamingBinaryBodyReactive")
	Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(@RequestBody Publisher<ByteBuffer> body);

	@GetMapping(path = "/urlNotSubstituted")
	Mono<String> urlNotSubstituted();

	@GetMapping(path = "/empty")
	Mono<TestObject> empty();

	@GetMapping(path = "/encode")
	Mono<TestObject> encodeParam(@RequestParam("id") String param);

	@GetMapping(path = "/encode/{id}")
	Mono<TestObject> encodePath(@PathVariable("id") String param);

	@GetMapping(path = "/expand/{date}")
	Mono<TestObject> expandPathParameter(@PathVariable("date") String date);

	@GetMapping(path = "/expand")
	Mono<TestObject> expandRequestParameter(@RequestParam("dateTime") String date);

	@GetMapping(path = "/Invoices")
	Mono<TestObject> expandPathParameterInRequestParameter(@RequestParam("filter") String filter);

	@PostMapping(path = "/formDataMap",
			consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	Mono<AllFeaturesApi.TestObject> formDataMap(ServerWebExchange serverWebExchange);

	@PostMapping(path = "/formDataParameters",
			consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	Mono<AllFeaturesApi.TestObject> formDataParameters(ServerWebExchange serverWebExchange);

}
