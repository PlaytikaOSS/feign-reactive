package reactivefeign.client.metrics;

import feign.MethodMetadata;
import feign.RequestTemplate;
import feign.Target;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.log.ReactiveLoggerListener;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static reactivefeign.client.metrics.MetricsTag.*;

/**
 * Micrometer implementation
 *
 * @author Sergii Karpenko
 */
public class MicrometerReactiveLogger implements ReactiveLoggerListener<MicrometerReactiveLogger.MetricsContext>{

    public static final String DEFAULT_TIMER_NAME = "reactive.feign.client.requests";

    private final Clock clock;
    private MeterRegistry meterRegistry;
    private String name;
    private Set<MetricsTag> tags;

    public static MicrometerReactiveLogger basicTimer(){
        return new MicrometerReactiveLogger(Clock.systemUTC(), Metrics.globalRegistry, DEFAULT_TIMER_NAME,
                MetricsTag.getMandatory());
    }

    public MicrometerReactiveLogger(Clock clock, MeterRegistry meterRegistry, String name, Set<MetricsTag> tags) {
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.name = name;
        this.tags = tags;

    }

    @Override
    public MetricsContext requestStarted(ReactiveHttpRequest request, Target<?> target, MethodMetadata methodMetadata) {
        return new MetricsContext(request, target, methodMetadata, clock);
    }

    @Override
    public void responseReceived(ReactiveHttpResponse<?> response, MetricsContext logContext) {
        meterRegistry.timer(name, buildResponseTags(response, logContext, tags))
                .record(logContext.timeSpent(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void errorReceived(Throwable throwable, MetricsContext logContext) {
        meterRegistry.timer(name, buildErrorTags(throwable, logContext, tags))
                .record(logContext.timeSpent(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean logRequestBody() {
        return false;
    }

    @Override
    public void bodySent(Object body, MetricsContext logContext) {
    }

    @Override
    public boolean logResponseBody() {
        return false;
    }

    @Override
    public void bodyReceived(Object body, MetricsContext logContext) {
    }

    static class MetricsContext {
        private final ReactiveHttpRequest request;
        private final Target<?> target;
        private final MethodMetadata methodMetadata;
        private final Clock clock;
        private final long startTime;

        public MetricsContext(ReactiveHttpRequest request, Target<?> target, MethodMetadata methodMetadata, Clock clock) {
            this.request = request;
            this.target = target;
            this.methodMetadata = methodMetadata;
            this.clock = clock;
            this.startTime = clock.millis();
        }

        public long timeSpent(){
            return clock.millis() - startTime;
        }

    }

    private static List<Tag> buildResponseTags(
            ReactiveHttpResponse<?> response,
            MetricsContext metricsContext, Set<MetricsTag> tags){
        List<Tag> metricsTags = buildTags(metricsContext, tags);
        //takes actual host resolved from service name
        if(tags.contains(HOST)){
            metricsTags.add(Tag.of(HOST.getTagName(), response.request().uri().getHost()));
        }
        metricsTags.add(Tag.of(STATUS.getTagName(), Integer.toString(response.status())));

        if(tags.contains(EXCEPTION)) {
            metricsTags.add(Tag.of(EXCEPTION.getTagName(), "None"));
        }

        return metricsTags;
    }

    private static List<Tag> buildErrorTags(
            Throwable ex,
            MetricsContext metricsContext, Set<MetricsTag> tags){
        List<Tag> metricsTags = buildTags(metricsContext, tags);
        if(tags.contains(HOST)){
            //takes actual host resolved from service name
            if(ex instanceof ReactiveFeignException){
                metricsTags.add(Tag.of(HOST.getTagName(), ((ReactiveFeignException)ex).getRequest().uri().getHost()));
            } else {
                metricsTags.add(Tag.of(HOST.getTagName(), metricsContext.request.uri().getHost()));
            }
        }
        metricsTags.add(Tag.of(STATUS.getTagName(), Integer.toString(-1)));

        if(tags.contains(EXCEPTION)) {
            metricsTags.add(Tag.of(EXCEPTION.getTagName(), ex.getClass().getSimpleName()));
        }

        return metricsTags;
    }

    private static List<Tag> buildTags(MetricsContext metricsContext, Set<MetricsTag> tags) {
        RequestTemplate requestTemplate = metricsContext.methodMetadata.template();
        List<Tag> metricsTags = new ArrayList<>(values().length);
        metricsTags.add(Tag.of(FEIGN_CLIENT_METHOD.getTagName(), metricsContext.methodMetadata.configKey()));
        if(tags.contains(URI_TEMPLATE)) {
            metricsTags.add(Tag.of(URI_TEMPLATE.getTagName(), metricsContext.target.url() + requestTemplate.url()));
        }
        if(tags.contains(HTTP_METHOD)) {
            metricsTags.add(Tag.of(HTTP_METHOD.getTagName(), requestTemplate.method()));
        }
        return metricsTags;
    }

}
