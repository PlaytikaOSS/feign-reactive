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
import feign.template.UriUtils;
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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
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

    public static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private final Target<?> target;
    private final MethodMetadata methodMetadata;
    private final PublisherHttpClient publisherClient;
    private final Function<Substitutions, String> pathExpander;
    private final Map<String, List<Function<Substitutions, List<String>>>> headerExpanders;
    private final Map<String, Collection<String>> queriesAll;
    private final Map<String, List<Function<Substitutions, List<String>>>> queryExpanders;
    private final URI staticUri;

    public PublisherClientMethodHandler(Target<?> target,
                                        MethodMetadata methodMetadata,
                                        PublisherHttpClient publisherClient) {
        this.target = checkNotNull(target, "target must be not null");
        this.methodMetadata = checkNotNull(methodMetadata,
                "methodMetadata must be not null");
        this.publisherClient = checkNotNull(publisherClient, "client must be not null");
        RequestTemplate requestTemplate = methodMetadata.template();
        this.pathExpander = buildUrlExpandFunction(requestTemplate, target);
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
    public Publisher<?> invoke(final Object[] argv) {
        return publisherClient.executeRequest(buildRequest(argv));
    }

    protected ReactiveHttpRequest buildRequest(Object[] argv) {

        Substitutions substitutions = buildSubstitutions(argv);

        URI uri = buildUri(argv, substitutions);

        Map<String, List<String>> headers = headers(argv, substitutions);

        return new ReactiveHttpRequest(methodMetadata, target, uri, headers, body(argv));
    }

    private URI buildUri(Object[] argv, Substitutions substitutions) {
        //static template
        if(staticUri != null){
            return staticUri;
        }

        String path = pathExpander.apply(substitutions);

        Map<String, Collection<String>> queries = queries(argv, substitutions);
        String queryLine = queryLine(queries);

        return URI.create(path + queryLine);
    }

    private Substitutions buildSubstitutions(Object[] argv) {
        Map<String, Object> substitutions = methodMetadata.indexToName().entrySet().stream()
                .filter(e -> argv[e.getKey()] != null)
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), v)))
                .collect(toMap(Map.Entry::getValue,
                        entry -> argv[entry.getKey()]));

        return new Substitutions(substitutions,
                methodMetadata.urlIndex() != null ? (URI)argv[methodMetadata.urlIndex()] : null);
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
                    queryBuilder.append(UriUtils.encode(value, UTF_8));
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
                                                      Substitutions substitutions) {
        Map<String, Collection<String>> queries = new LinkedHashMap<>();

        // queries from template
        queriesAll.keySet()
                .forEach(queryName -> addAll(queries, queryName,
                        queryExpanders.get(queryName).stream()
                                .map(expander -> expander.apply(substitutions))
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

    protected Map<String, List<String>> headers(Object[] argv, Substitutions substitutions) {

        Map<String, List<String>> headers = new LinkedHashMap<>();

        // headers from template
        methodMetadata.template().headers().keySet()
                .forEach(headerName -> addAllOrdered(headers, headerName,
                        headerExpanders.get(headerName).stream()
                                .map(expander -> expander.apply(substitutions))
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

    private static Map<String, List<Function<Substitutions, List<String>>>> buildExpanders(
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
    private static Function<Substitutions, List<String>> buildExpandFunction(String template) {
        Matcher matcher = SUBSTITUTION_PATTERN.matcher(template);
        if(matcher.matches()){
            String placeholder = matcher.group(1);

            return substitutions -> {
                Object substitution = substitutions.placeholderToSubstitution.get(placeholder);
                if (substitution != null) {
                    if(substitution instanceof Iterable){
                        List<String> stringValues = new ArrayList<>();
                        ((Iterable<?>) substitution).forEach(o -> stringValues.add(o.toString()));
                        return stringValues;
                    } else if(substitution instanceof Object[]){
                        List<String> stringValues = new ArrayList<>(((Object[]) substitution).length);
                        (asList((Object[])substitution)).forEach(o -> stringValues.add(o.toString()));
                        return stringValues;
                    }
                    else {
                        return singletonList(substitution.toString());
                    }
                } else {
                    return null;
                }
            };
        } else {
            return substitutions -> singletonList(template);
        }
    }

    private static Function<Substitutions, String> buildUrlExpandFunction(
            RequestTemplate requestTemplate, Target<?> target) {
        if(target instanceof Target.EmptyTarget){
            return expandUrlForEmptyTarget(buildUrlExpandFunction(
                    cutTail(requestTemplate.url(), requestTemplate.queryLine())));
        } else {
            return buildUrlExpandFunction(target.url() +
                    cutTail(requestTemplate.url(), requestTemplate.queryLine()));
        }
    }

    private static Function<Substitutions, String> expandUrlForEmptyTarget(
            Function<Substitutions, String> expandFunction){
        return substitutions -> substitutions.url.toString() + expandFunction.apply(substitutions);
    }

    /**
     *
     * @param template
     * @return function that able to map substitutions map to actual value for specified template
     */
    private static Function<Substitutions, String> buildUrlExpandFunction(String template) {
        List<Function<Substitutions, String>> chunks = new ArrayList<>();
        Matcher matcher = SUBSTITUTION_PATTERN.matcher(template);
        int previousMatchEnd = 0;
        while (matcher.find()) {
            String textChunk = template.substring(previousMatchEnd, matcher.start());
            if (textChunk.length() > 0) {
                chunks.add(data -> textChunk);
            }

            String placeholder = matcher.group(1);
            chunks.add(data -> {
                Object substitution = data.placeholderToSubstitution.get(placeholder);
                if (substitution != null) {
                    return UriUtils.encode(substitution.toString(), UTF_8);
                } else {
                    throw new IllegalArgumentException("No substitution in url for:"+placeholder);
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

        return substitutions -> chunks.stream().map(chunk -> chunk.apply(substitutions))
                .collect(Collectors.joining());
    }

    private static class StaticPathExpander implements Function<Substitutions, String>{

        private final String staticPath;

        private StaticPathExpander(String staticPath) {
            this.staticPath = staticPath;
        }

        @Override
        public String apply(Substitutions substitutions) {
            return staticPath;
        }
    }

    private static class Substitutions {
        private final URI url;
        private final Map<String, Object> placeholderToSubstitution;

        private Substitutions(Map<String, Object> placeholderToSubstitution, URI url) {
            this.url = url;
            this.placeholderToSubstitution = placeholderToSubstitution;
        }
    }

}
