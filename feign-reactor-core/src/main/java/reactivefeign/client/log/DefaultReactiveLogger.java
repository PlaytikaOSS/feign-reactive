package reactivefeign.client.log;

import org.slf4j.LoggerFactory;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.utils.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Default slf4j implementation
 *
 * @author Sergii Karpenko
 */
public class DefaultReactiveLogger implements ReactiveLoggerListener<DefaultReactiveLogger.LogContext>{

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultReactiveLogger.class);

    private final Clock clock;

    public DefaultReactiveLogger(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LogContext requestStarted(ReactiveHttpRequest request, String feignMethodTag) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}]--->{} {} HTTP/1.1", feignMethodTag, request.method(),
                    request.uri());
        }

        if (logger.isTraceEnabled()) {
            logRequestHeaders(request, feignMethodTag);
        }

        return new LogContext(clock);
    }

    @Override
    public boolean logRequestBody() {
        return logger.isTraceEnabled();
    }

    private void logRequestHeaders(ReactiveHttpRequest request, String feignMethodTag) {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] REQUEST HEADERS\n{}", feignMethodTag,
                    msg(() -> request.headers().entrySet().stream()
                            .map(entry -> String.format("%s:%s", entry.getKey(),
                                    entry.getValue()))
                            .collect(Collectors.joining("\n"))));
        }
    }

    @Override
    public void bodySent(ReactiveHttpRequest request, String feignMethodTag, LogContext logContext, Object body) {
        if (logger.isTraceEnabled()) {
            if (request.body() instanceof Mono) {
                logger.trace("[{}] REQUEST BODY\n{}", feignMethodTag, body);
            } else if (request.body() instanceof Flux) {
                logger.trace("[{}] REQUEST BODY ELEMENT\n{}", feignMethodTag, body);
            } else {
                throw new IllegalArgumentException("Unsupported publisher type: " + request.body().getClass());
            }
        }
    }

    @Override
    public void responseReceived(ReactiveHttpResponse response, String feignMethodTag, LogContext logContext) {
        logResponseHeaders(response, feignMethodTag, logContext.timeSpent());
    }

    @Override
    public boolean logResponseBody() {
        return logger.isTraceEnabled();
    }

    private void logResponseHeaders(ReactiveHttpResponse httpResponse, String feignMethodTag,
                                    long elapsedTime) {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] RESPONSE HEADERS\n{}", feignMethodTag,
                    msg(() -> httpResponse.headers().entrySet().stream()
                            .flatMap(entry -> entry.getValue().stream()
                                    .map(value -> new Pair<>(entry.getKey(), value)))
                            .map(pair -> String.format("%s:%s", pair.left, pair.right))
                            .collect(Collectors.joining("\n"))));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[{}]<--- headers takes {} milliseconds", feignMethodTag,
                    elapsedTime);
        }
    }

    @Override
    public void bodyReceived(ReactiveHttpResponse response, String feignMethodTag, LogContext logContext, Object body) {
        if (logger.isTraceEnabled()) {
            if(response.body() instanceof Mono) {
                logger.trace("[{}] RESPONSE BODY\n{}", feignMethodTag, body);
            } else {
                logger.trace("[{}] RESPONSE BODY ELEMENT\n{}", feignMethodTag, body);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("[{}]<--- body takes {} milliseconds", feignMethodTag, logContext.timeSpent());
        }
    }

    static class LogContext{
        private final Clock clock;
        private final long startTime;

        public LogContext(Clock clock) {
            this.clock = clock;
            this.startTime = clock.millis();
        }

        public long timeSpent(){
            return clock.millis() - startTime;
        }
    }

    private static MessageSupplier msg(Supplier<?> supplier) {
        return new MessageSupplier(supplier);
    }

    static class MessageSupplier {
        private Supplier<?> supplier;

        public MessageSupplier(Supplier<?> supplier) {
            this.supplier = supplier;
        }

        @Override
        public String toString() {
            return supplier.get().toString();
        }
    }
}
