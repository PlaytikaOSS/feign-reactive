package reactivefeign.client.log;

import feign.MethodMetadata;
import feign.Target;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.utils.Pair;
import reactivefeign.utils.SerializedFormData;
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

    private final org.slf4j.Logger logger;

    private final Clock clock;

    public DefaultReactiveLogger(Clock clock) {
        this(clock, LoggerFactory.getLogger(DefaultReactiveLogger.class));
    }
    
    public DefaultReactiveLogger(Clock clock, org.slf4j.Logger logger) {
      this.clock = clock;
      this.logger = logger;
    }

    @Override
    public LogContext requestStarted(ReactiveHttpRequest request, Target target, MethodMetadata methodMetadata) {
        LogContext logContext = new LogContext(request, target, methodMetadata, clock);

        if (logger.isDebugEnabled()) {
            logger.debug("[{}]--->{} {} HTTP/1.1", logContext.feignMethodKey, request.method(),
                    request.uri());
        }

        if (logger.isTraceEnabled()) {
            logRequestHeaders(request, logContext.feignMethodKey);
        }

        return logContext;
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
    public void bodySent(Object body, LogContext logContext) {
        if (logger.isTraceEnabled()) {
            Publisher<Object> requestBody = logContext.request.body();
            if (requestBody instanceof Mono) {
                logger.trace("[{}] REQUEST BODY\n{}", logContext.feignMethodKey, body);
            } else if (requestBody instanceof Flux) {
                logger.trace("[{}] REQUEST BODY ELEMENT\n{}", logContext.feignMethodKey, body);
            } else if(requestBody instanceof SerializedFormData){
                logger.trace("[{}] REQUEST BODY FORM DATA\n{}", logContext.feignMethodKey, body);
            }
            else {
                throw new IllegalArgumentException("Unsupported publisher type: " + requestBody.getClass());
            }
        }
    }

    @Override
    public void responseReceived(ReactiveHttpResponse response, LogContext logContext) {
        logContext.setResponse(response);
        logResponseHeaders(response, logContext.feignMethodKey, logContext.timeSpent());
    }

    @Override
    public void errorReceived(Throwable throwable, LogContext logContext) {
        if (logger.isErrorEnabled()) {
            logger.error("[{}]--->{} {} HTTP/1.1", logContext.feignMethodKey,
                    logContext.request.method(), logContext.request.uri(), throwable);
        }
    }

    @Override
    public boolean logResponseBody() {
        return logger.isTraceEnabled();
    }

    private void logResponseHeaders(ReactiveHttpResponse<?> httpResponse, String feignMethodTag,
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
    public void bodyReceived(Object body, LogContext logContext) {
        if (logger.isTraceEnabled()) {
            if(logContext.getResponse().body() instanceof Mono) {
                logger.trace("[{}] RESPONSE BODY\n{}", logContext.feignMethodKey, body);
            } else {
                logger.trace("[{}] RESPONSE BODY ELEMENT\n{}", logContext.feignMethodKey, body);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("[{}]<--- body takes {} milliseconds", logContext.feignMethodKey, logContext.timeSpent());
        }
    }

    static class LogContext{
        private final ReactiveHttpRequest request;
        private final Target target;
        private final MethodMetadata methodMetadata;
        private final Clock clock;
        private final long startTime;

        private final String feignMethodKey;

        private ReactiveHttpResponse response;

        public LogContext(ReactiveHttpRequest request, Target target, MethodMetadata methodMetadata, Clock clock) {
            this.request = request;
            this.target = target;
            this.methodMetadata = methodMetadata;
            this.clock = clock;
            this.startTime = clock.millis();
            this.feignMethodKey = methodMetadata.configKey();
        }

        public long timeSpent(){
            return clock.millis() - startTime;
        }

        public ReactiveHttpResponse getResponse() {
            return response;
        }

        public void setResponse(ReactiveHttpResponse response) {
            this.response = response;
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
