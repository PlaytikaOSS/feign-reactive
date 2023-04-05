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

import feign.CollectionFormat;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Headers({ "Accept: application/json" })
public interface AllFeaturesFeign extends AllFeaturesApi{

	String DATE_TIME_FORMAT = "dd-MM-yyyy'T'HH:mm:ss";

	@Override
	@RequestLine("GET /mirrorParameters/{parameterInPathPlaceholder}?paramInUrl={paramInQueryPlaceholder}")
	Mono<Map<String, String>> mirrorParameters(
            @Param("parameterInPathPlaceholder") long paramInPath,
            @Param("paramInQueryPlaceholder") String paramInQuery,
            @QueryMap Map<String, String> paramMap);

	@Override
	@RequestLine("GET /mirrorParametersNew?paramInUrl={paramInUrlPlaceholder}")
	Mono<Map<String, String>> mirrorParametersNew(
            @Param("paramInUrlPlaceholder") long paramInUrl,
            @Param("dynamicParam") Long dynamicParam,
            @QueryMap Map<String, String> paramMap);

	@Override
	@RequestLine("GET /mirrorParametersInUrl?manufacturingPlan=ZPH-V121-00123&workOrder=")
	Mono<Map<String, String>> passEmptyParameterInUrl();

	@Override
	@RequestLine(value = "GET /mirrorListParametersNew", collectionFormat = CollectionFormat.CSV)
	Mono<List<Integer>> mirrorListParametersNew(
            @Param("dynamicListParam") List<Integer> dynamicListParam);

	@Override
	@RequestLine("GET /mirrorArrayParametersNew")
	Mono<String[]> mirrorArrayParametersNew(
			@Param("dynamicArrayParam") String[] dynamicArrayParam);

	@Override
	@RequestLine("GET /mirrorMapParametersNew")
	Mono<Map<String, List<String>>> mirrorMapParametersNew(
            @QueryMap Map<String, List<String>> paramMap);

	@Override
	@RequestLine("GET /mirrorHeaders")
	@Headers({ "Method-Header: {headerValue}" })
	Mono<Map<String, String>> mirrorHeaders(@Param("headerValue") long param,
                                            @HeaderMap Map<String, String> paramMap);

	@Override
	@RequestLine("GET /mirrorListHeaders")
	@Headers({ "Method-Header: {headerValue}" })
	Mono<List<Long>> mirrorListHeaders(
            @Param("headerValue") List<Long> param);

	@Override
	@RequestLine("GET /mirrorMultiMapHeaders")
	Mono<Map<String, List<String>>> mirrorMultiMapHeaders(
            @HeaderMap Map<String, List<String>> headerMap);

	@Override
	@RequestLine("GET /mirrorHeaderAndRequestWithSameName")
	@Headers({ "username: {headerValue}" })
	Mono<String[]> mirrorHeaderAndRequestWithSameName(
			@Param("headerValue") String header,
			@Param("username") String requestParam);

	@Override
	@RequestLine("POST " + "/mirrorBody")
	Mono<String> mirrorBody(String body);

	@Override
	@RequestLine("POST " + "/mirrorBodyMap")
	@Headers({ "Content-Type: application/json" })
	Mono<Map<String, String>> mirrorBodyMap(Map<String, String> body);

	@Override
	@RequestLine("POST " + "/mirrorBodyReactive")
	@Headers({ "Content-Type: application/json" })
	Mono<String> mirrorBodyReactive(Publisher<String> body);

	@Override
	@RequestLine("POST " + "/mirrorBodyMapReactive")
	@Headers({ "Content-Type: application/json" })
	Mono<Map<String, String>> mirrorBodyMapReactive(Publisher<Map<String, String>> body);

	@Override
	@RequestLine("POST " + "/mirrorBodyStream")
	@Headers({ "Content-Type: "+APPLICATION_STREAM_JSON_VALUE,
			   "Accept: "+APPLICATION_STREAM_JSON_VALUE})
	Flux<TestObject> mirrorBodyStream(Publisher<TestObject> bodyStream);

	@Override
	@RequestLine("POST " + "/mirrorIntegerBodyStream")
	@Headers({ "Content-Type: "+APPLICATION_STREAM_JSON_VALUE,
			"Accept: "+APPLICATION_STREAM_JSON_VALUE})
	Flux<Integer> mirrorIntegerBodyStream(Flux<Integer> body);

	@Override
	@RequestLine("POST " + "/mirrorStringBodyStream")
	@Headers({ "Content-Type: "+TEXT_EVENT_STREAM_VALUE,
			"Accept: "+TEXT_EVENT_STREAM_VALUE})
	Flux<String> mirrorStringBodyStream(Flux<String> body);

	@Override
	@RequestLine("GET /empty")
	@Headers({ "Method-Header: {headerValue}" })
	Mono<TestObject> empty();

	@Override
	@RequestLine("POST " + "/mirrorBodyWithDelay")
	Mono<String> mirrorBodyWithDelay(String body);

	@Override
	@RequestLine("POST " + "/mirrorStreamingBinaryBodyReactive")
	@Headers({ "Content-Type: "+APPLICATION_OCTET_STREAM_VALUE })
	Flux<ByteBuffer> mirrorStreamingBinaryBodyReactive(Publisher<ByteBuffer> body);

	@Override
	@RequestLine("GET /urlNotSubstituted/{parameterInPathPlaceholder}")
	Mono<String> urlNotSubstituted();

	default Mono<String> mirrorDefaultBody() {
		return mirrorBody("default");
	}

	@Override
	@RequestLine("GET /encode")
    Mono<TestObject> encodeParam(@Param("id") String param);

	@Override
	@RequestLine("GET /encode/{id}")
    Mono<TestObject> encodePath(@Param("id") String param);

	@Override
	@RequestLine("GET /expand/{timestamp}")
	Mono<TestObject> expandPathParameter(@Param(value = "timestamp", expander = TimestampToDateExpander.class) long timestamp);

	@Override
	@RequestLine("GET /Invoices?filter=Company:{companyName}")
	Mono<TestObject> expandPathParameterInRequestParameter(@Param("companyName") String companyName);

	@Override
	@RequestLine("GET /expand")
	Mono<TestObject> expandDataTimeParameterWithCustomFormat(
			@Param(value = "dateTime", expander = LocalDateTimeExpander.class) LocalDateTime dateTime);

	@Override
	@Headers({ "Content-Type: application/x-www-form-urlencoded" })
	@RequestLine("POST /formDataMap")
	Mono<TestObject> formDataMap(Map<String, ?> form);

	@Headers({"Content-Type: application/x-www-form-urlencoded",
			"Accept: " + APPLICATION_JSON_VALUE})
	@RequestLine("POST " + "/formDataParameters")
	Mono<TestObject> formDataParameters(
			@Param("key1") String organizationName,
			@Param("key2") String organizationId);

	class TimestampToDateExpander implements Param.Expander {

		@Override
		public String expand(Object value) {
			return new Date((Long)value).toString();
		}
	}

	class LocalDateTimeExpander implements Param.Expander {

		@Override
		public String expand(Object value) {
			return ((LocalDateTime)value).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
		}
	}
}
