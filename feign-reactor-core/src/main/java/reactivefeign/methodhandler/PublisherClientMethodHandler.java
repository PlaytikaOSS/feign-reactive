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
package reactivefeign.methodhandler;

import feign.MethodMetadata;
import feign.RequestTemplate;
import feign.Target;
import org.reactivestreams.Publisher;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.publisher.PublisherHttpClient;
import reactivefeign.utils.Pair;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static feign.Util.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static reactivefeign.utils.MultiValueMapUtils.*;
import static reactivefeign.utils.StringUtils.cutTail;

/**
 * Method handler for asynchronous HTTP requests via {@link PublisherHttpClient}.
 *
 * Transforms method invocation into request that executed by {@link ReactiveHttpClient}.
 *
 * @author Sergii Karpenko
 */
public class PublisherClientMethodHandler implements MethodHandler {

    /**
     *
     * @param template
     * @return function that able to map substitutions map to actual value for specified template
     */
    public static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final Target target;
    private final MethodMetadata methodMetadata;
    private final PublisherHttpClient publisherClient;
    private final Function<Map<String, ?>, String> pathExpander;
    private final Map<String, List<Function<Map<String, ?>, List<String>>>> headerExpanders;
    private final Map<String, Collection<String>> queriesAll;
    private final Map<String, List<Function<Map<String, ?>, List<String>>>> queryExpanders;
    private final URI staticUri;

    public PublisherClientMethodHandler(Target target,
                                        MethodMetadata methodMetadata,
                                        PublisherHttpClient publisherClient) {
        this.target = checkNotNull(target, "target must be not null");
        this.methodMetadata = checkNotNull(methodMetadata,
                "methodMetadata must be not null");
        this.publisherClient = checkNotNull(publisherClient, "client must be not null");
        RequestTemplate requestTemplate = methodMetadata.template();
        this.pathExpander = buildUrlExpandFunction(target.url() +
                cutTail(requestTemplate.url(), requestTemplate.queryLine()));
        this.headerExpanders = buildExpanders(requestTemplate.headers());

        this.queriesAll = new HashMap<>(requestTemplate.queries());
        methodMetadata.formParams()
                .forEach(param -> add(queriesAll, param, "{" + param + "}"));
        this.queryExpanders = buildExpanders(queriesAll);

        //static template (POST & PUT)
        if(pathExpander instanceof StaticPathExpander
                && queriesAll.isEmpty()
                && methodMetadata.queryMapIndex() == null){
            staticUri = URI.create(target.url() + requestTemplate.url());
        } else {
            staticUri = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<?> invoke(final Object[] argv) {

        final ReactiveHttpRequest request = buildRequest(argv);

        return publisherClient.executeRequest(request);
    }

    protected ReactiveHttpRequest buildRequest(Object[] argv) {

        Map<String, ?> substitutionsMap = buildSubstitutions(argv);

        URI uri = buildUri(argv, substitutionsMap);

        Map<String, List<String>> headers = headers(argv, substitutionsMap);

        return new ReactiveHttpRequest(methodMetadata.template().method(), uri, headers, body(argv));
    }

    private URI buildUri(Object[] argv, Map<String, ?> substitutionsMap) {
        //static template
        if(staticUri != null){
            return staticUri;
        }

        String path = pathExpander.apply(substitutionsMap);

        Map<String, Collection<String>> queries = queries(argv, substitutionsMap);
        String queryLine = queryLine(queries);

        return URI.create(path + queryLine);
    }

    private Map<String, Object> buildSubstitutions(Object[] argv) {
        return methodMetadata.indexToName().entrySet().stream()
                .filter(e -> argv[e.getKey()] != null)
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
                .collect(Collectors.toMap(Map.Entry::getValue,
                        entry -> argv[entry.getKey()]));
    }

    private String queryLine(Map<String, Collection<String>> queries) {
        if (queries.isEmpty()) {
            return "";
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, Collection<String>> query : queries.entrySet()) {
            String field = query.getKey();
            for (String value : query.getValue()) {
                queryBuilder.append('&');
                queryBuilder.append(field);
                queryBuilder.append('=');
                if (!value.isEmpty()) {
                    queryBuilder.append(value);
                }
            }
        }
        if(queryBuilder.length() > 0) {
            queryBuilder.deleteCharAt(0);
            return queryBuilder.insert(0, '?').toString();
        } else {
            return "";
        }
    }

    protected Map<String, Collection<String>> queries(Object[] argv,
                                                      Map<String, ?> substitutionsMap) {
        Map<String, Collection<String>> queries = new LinkedHashMap<>();

        // queries from template
        queriesAll.keySet()
                .forEach(queryName -> addAll(queries, queryName,
                        queryExpanders.get(queryName).stream()
                                .map(expander -> expander.apply(substitutionsMap))
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .collect(toList())));

        // queries from args
        if (methodMetadata.queryMapIndex() != null && argv[methodMetadata.queryMapIndex()] != null) {
            ((Map<String, ?>) argv[methodMetadata.queryMapIndex()])
                    .forEach((key, value) -> {
                        if (value instanceof Iterable) {
                            ((Iterable<?>) value).forEach(element -> add(queries, key, element.toString()));
                        } else if(value != null){
                            add(queries, key, value.toString());
                        }
                    });
        }

        return queries;
    }

    protected Map<String, List<String>> headers(Object[] argv, Map<String, ?> substitutionsMap) {

        Map<String, List<String>> headers = new LinkedHashMap<>();

        // headers from template
        methodMetadata.template().headers().keySet()
                .forEach(headerName -> addAllOrdered(headers, headerName,
                        headerExpanders.get(headerName).stream()
                                .map(expander -> expander.apply(substitutionsMap))
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .collect(toList())));

        // headers from args
        if (methodMetadata.headerMapIndex() != null) {
            ((Map<String, ?>) argv[methodMetadata.headerMapIndex()])
                    .forEach((key, value) -> {
                        if (value instanceof Iterable) {
                            ((Iterable<?>) value)
                                    .forEach(element -> addOrdered(headers, key, element.toString()));
                        } else {
                            addOrdered(headers, key, value.toString());
                        }
                    });
        }

        return headers;
    }

    protected Publisher<Object> body(Object[] argv) {
        if (methodMetadata.bodyIndex() != null) {
            return body(argv[methodMetadata.bodyIndex()]);
        } else {
            return Mono.empty();
        }
    }

    protected Publisher<Object> body(Object body) {
        if (body instanceof Publisher) {
            return (Publisher<Object>) body;
        } else {
            return Mono.just(body);
        }
    }

    private static Map<String, List<Function<Map<String, ?>, List<String>>>> buildExpanders(
            Map<String, Collection<String>> templates) {
        Stream<Pair<String, String>> templatesFlattened = templates.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new Pair<>(e.getKey(), v)));
        return templatesFlattened.collect(groupingBy(
                entry -> entry.left,
                mapping(entry -> buildExpandFunction(entry.right), toList())));
    }

    /**
     *
     * @param template
     * @return function that able to map substitutions map to actual value for specified template
     */
    private static Function<Map<String, ?>, List<String>> buildExpandFunction(String template) {
        Matcher matcher = SUBSTITUTION_PATTERN.matcher(template);
        if(matcher.matches()){
            String substitute = matcher.group(1);

            return traceData -> {
                Object substitution = traceData.get(substitute);
                if (substitution != null) {
                    if(substitution instanceof Iterable){
                        List<String> stringValues = new ArrayList<>();
                        ((Iterable) substitution).forEach(o -> stringValues.add(o.toString()));
                        return stringValues;
                    } else {
                        return singletonList(substitution.toString());
                    }
                } else {
                    return null;
                }
            };
        } else {
            return traceData -> singletonList(template);
        }
    }

    /**
     *
     * @param template
     * @return function that able to map substitutions map to actual value for specified template
     */
    private static Function<Map<String, ?>, String> buildUrlExpandFunction(String template) {
        List<Function<Map<String, ?>, String>> chunks = new ArrayList<>();
        Matcher matcher = SUBSTITUTION_PATTERN.matcher(template);
        int previousMatchEnd = 0;
        while (matcher.find()) {
            String textChunk = template.substring(previousMatchEnd, matcher.start());
            if (textChunk.length() > 0) {
                chunks.add(data -> textChunk);
            }

            String substitute = matcher.group(1);
            chunks.add(data -> {
                Object substitution = data.get(substitute);
                if (substitution != null) {
                    return substitution.toString();
                } else {
                    throw new IllegalArgumentException("No substitution in url for:"+substitute);
                }
            });
            previousMatchEnd = matcher.end();
        }

        //no substitutions in path
        if(previousMatchEnd == 0){
            return new StaticPathExpander(template);
        }

        String textChunk = template.substring(previousMatchEnd);
        if (textChunk.length() > 0) {
            chunks.add(data -> textChunk);
        }

        return traceData -> chunks.stream().map(chunk -> chunk.apply(traceData))
                .collect(Collectors.joining());
    }

    private static class StaticPathExpander implements Function<Map<String, ?>, String>{

        private final String staticPath;

        private StaticPathExpander(String staticPath) {
            this.staticPath = staticPath;
        }

        @Override
        public String apply(Map<String, ?> stringMap) {
            return staticPath;
        }
    }

}
