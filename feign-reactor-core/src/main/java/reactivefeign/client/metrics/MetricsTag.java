package reactivefeign.client.metrics;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Patterned by https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-metrics.html#production-ready-metrics-http-clients
 */
public enum MetricsTag {

    FEIGN_CLIENT_METHOD("feignMethod", true),
    URI_TEMPLATE("uri", false), //in form of (serviceName|host)/pathTemplate?queryTemplate
    HTTP_METHOD("method", false),
    HOST("host", false), //host resolved from serviceName
    STATUS("status", true),
    EXCEPTION("exception", false);

    private String tagName;
    private boolean mandatory;

    MetricsTag(String tagName, boolean mandatory) {
        this.tagName = tagName;
        this.mandatory = mandatory;
    }

    public String getTagName() {
        return tagName;
    }

    public static Set<MetricsTag> getMandatory(){
        return EnumSet.copyOf(Stream.of(MetricsTag.values())
                .filter(metricsTag -> metricsTag.mandatory)
                .collect(Collectors.toList()));
    }
}
