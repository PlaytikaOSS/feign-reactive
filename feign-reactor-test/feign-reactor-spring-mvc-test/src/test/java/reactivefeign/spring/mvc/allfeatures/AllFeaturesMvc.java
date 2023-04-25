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

package reactivefeign.spring.mvc.allfeatures;

import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.CollectionFormat;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static reactivefeign.allfeatures.AllFeaturesFeign.DATE_TIME_FORMAT;

public interface AllFeaturesMvc extends reactivefeign.allfeatures.AllFeaturesApi {

	@Override
	@GetMapping(path = "/mirrorParameters/{paramInPath}")
	Mono<Map<String, String>> mirrorParameters(
			@PathVariable("paramInPath") long paramInPath,
			@RequestParam("paramInUrl") String paramInUrl,
			@RequestParam Map<String, String> paramMap);

	@GetMapping(path = "/mirrorParameters/{paramInPath}")
	Mono<Map<String, String>> mirrorParametersViaSpringQueryMap(
			@PathVariable("paramInPath") long paramInPath,
			@RequestParam("paramInUrl") String paramInUrl,
			@SpringQueryMap Map<String, String> paramMap);

	@Override
	@GetMapping(path = "/mirrorParametersNew")
	Mono<Map<String, String>> mirrorParametersNew(
			@RequestParam("paramInUrl") long paramInUrl,
			@RequestParam(value = "dynamicParam", required = false) Long dynamicParam,
			@RequestParam Map<String, String> paramMap);

	@Override
	@GetMapping(path = "/mirrorListParametersNew")
	@CollectionFormat(feign.CollectionFormat.CSV)
	Mono<List<Integer>> mirrorListParametersNew(
			@RequestParam(value = "dynamicListParam", required = false) List<Integer> listParams);

	@Override
	@GetMapping(path = "/mirrorArrayParametersNew")
	Mono<String[]> mirrorArrayParametersNew(
			@RequestParam("dynamicArrayParam") String[] dynamicArrayParam);

	//Spring can't treat Map<String, List<String>> correctly
	@Override
	@GetMapping(path = "/mirrorMapParametersNew")
	Mono<Map<String, List<String>>> mirrorMapParametersNew(
			@RequestParam Map<String, List<String>> paramMap);

	@Override
	@GetMapping(path = "/mirrorParametersInUrl?manufacturingPlan=ZPH-V121-00123&workOrder=")
	Mono<Map<String, String>> passEmptyParameterInUrl();

	@Override
	@GetMapping(path = "/mirrorHeaders")
	Mono<Map<String, String>> mirrorHeaders(
			@RequestHeader("Method-Header") long param,
			@RequestHeader Map<String, String> headersMap);

	@Override
	@GetMapping(path = "/mirrorListHeaders")
	Mono<List<Long>> mirrorListHeaders(
			@RequestHeader("Method-Header") List<Long> param);

	//Spring can't treat Map<String, List<String>> correctly
	@Override
	@GetMapping(path = "/mirrorMultiMapHeaders")
	Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
			@RequestHeader Map<String, List<String>> param);

	@Override
	@GetMapping(path = "/mirrorHeaderAndRequestWithSameName")
	Mono<String[]> mirrorHeaderAndRequestWithSameName(
			@RequestHeader("username") String header,
			@RequestParam("username") String requestParam);

	@Override
	@PostMapping(path = "/mirrorBody")
	Mono<String> mirrorBody(@RequestBody String body);

	@Override
	@PostMapping(path = "/mirrorBodyMap")
	Mono<Map<String, String>> mirrorBodyMap(
			@RequestBody Map<String, String> body);

	@Override
	@PostMapping(path = "/mirrorBodyReactive")
	Mono<String> mirrorBodyReactive(@RequestBody Publisher<String> body);

	@Override
	@PostMapping(path = "/mirrorBodyMapReactive")
	Mono<Map<String, String>> mirrorBodyMapReactive(
			@RequestBody Publisher<Map<String, String>> body);

	@Override
	@PostMapping(path = "/mirrorBodyStream")
	Flux<TestObject> mirrorBodyStream(
			@RequestBody Publisher<TestObject> bodyStream);

	@Override
	@PostMapping(path = "/mirrorIntegerBodyStream")
	Flux<Integer> mirrorIntegerBodyStream(
			@RequestBody Flux<Integer> body);

	@Override
	@PostMapping(path = "/mirrorStringBodyStream")
	Flux<String> mirrorStringBodyStream(
			@RequestBody  Flux<String> body);

	@Override
	@PostMapping(path = "/mirrorBodyWithDelay")
	Mono<String> mirrorBodyWithDelay(@RequestBody String body);

	@Override
	@PostMapping(path = "/mirrorStreamingBinaryBodyReactive")
	Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(@RequestBody Publisher<ByteBuffer> body);

	@Override
	@GetMapping(path = "/urlNotSubstituted")
	Mono<String> urlNotSubstituted();

	@Override
	@GetMapping(path = "/empty")
	Mono<TestObject> empty();

	@Override
	@GetMapping(path = "/encode")
	Mono<TestObject> encodeParam(@RequestParam("id") String param);

	@Override
	@GetMapping(path = "/encode/{id}")
	Mono<TestObject> encodePath(@PathVariable("id") String param);

	@Override
	@GetMapping(path = "/expand/{timestamp}")
	Mono<TestObject> expandPathParameter(@PathVariable("timestamp") long timestamp);

	@Override
	@GetMapping(path = "/Invoices?filter=Company:{companyName}")
	Mono<TestObject> expandPathParameterInRequestParameter(@RequestParam("companyName") String companyName);

	@GetMapping(path = "/expand")
	Mono<TestObject> expandDataTimeParameterWithCustomFormat(
			@DateTimeFormat(pattern = DATE_TIME_FORMAT)
			@RequestParam("dateTime") LocalDateTime dateTime);


	@PostMapping(path = "/formDataMap",
			consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	Mono<TestObject> formDataMap(Map<String, ?> form);

	@PostMapping(path = "/formDataParameters",
			consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	Mono<TestObject> formDataParameters(
			@RequestPart("key1") String organizationName,
			@RequestPart("key2") String organizationId);

}
