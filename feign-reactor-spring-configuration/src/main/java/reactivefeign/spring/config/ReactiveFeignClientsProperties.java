package reactivefeign.spring.config;

import feign.Contract;
import feign.codec.ErrorDecoder;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.ReactiveErrorMapper;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.client.metrics.MicrometerReactiveLogger;
import reactivefeign.client.statushandler.ReactiveStatusHandler;
import reactivefeign.retry.ReactiveRetryPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReactiveFeignClientsProperties<O extends ReactiveOptions.Builder> {

    private boolean defaultToProperties = true;

    private String defaultConfig = "default";

    private Map<String, ReactiveFeignClientProperties<O>> config = new HashMap<>();

    public boolean isDefaultToProperties() {
        return defaultToProperties;
    }

    public void setDefaultToProperties(boolean defaultToProperties) {
        this.defaultToProperties = defaultToProperties;
    }

    public String getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(String defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public Map<String, ReactiveFeignClientProperties<O>> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ReactiveFeignClientProperties<O>> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactiveFeignClientsProperties that = (ReactiveFeignClientsProperties) o;
        return defaultToProperties == that.defaultToProperties &&
                Objects.equals(defaultConfig, that.defaultConfig) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultToProperties, defaultConfig, config);
    }

    public static class ReactiveFeignClientProperties<O extends ReactiveOptions.Builder> {

        private Boolean defaultToProperties;

        private O options;

        //used for no cloud configuration
        private RetryProperties retry;
        private RetryProperties retryOnSame;
        private RetryProperties retryOnNext;

        private Class<ReactiveStatusHandler> statusHandler;

        private Class<ReactiveErrorMapper> errorMapper;

        private Class<ErrorDecoder> errorDecoder;

        private List<Class<ReactiveHttpRequestInterceptor>> requestInterceptors;

        private Class<ReactiveLoggerListener> logger;

        private Class<MicrometerReactiveLogger> metricsLogger;

        private Boolean decode404;

        private Class<Contract> contract;

        private Map<String, List<String>> defaultRequestHeaders;

        private Map<String, List<String>> defaultQueryParameters;

        public Boolean getDefaultToProperties() {
            return defaultToProperties;
        }

        public void setDefaultToProperties(Boolean defaultToProperties) {
            this.defaultToProperties = defaultToProperties;
        }

        public O getOptions() {
            return options;
        }

        public void setOptions(O options) {
            this.options = options;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public RetryProperties getRetryOnSame() {
            return retryOnSame;
        }

        public RetryProperties getRetryOnNext() {
            return retryOnNext;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }

        public void setRetryOnSame(RetryProperties retryOnSame) {
            this.retryOnSame = retryOnSame;
        }

        public void setRetryOnNext(RetryProperties retryOnNext) {
            this.retryOnNext = retryOnNext;
        }

        public Class<ReactiveStatusHandler> getStatusHandler() {
            return statusHandler;
        }

        public void setStatusHandler(Class<ReactiveStatusHandler> statusHandler) {
            this.statusHandler = statusHandler;
        }

        public Class<ReactiveErrorMapper> getErrorMapper() {
            return errorMapper;
        }

        public void setErrorMapper(Class<ReactiveErrorMapper> errorMapper) {
            this.errorMapper = errorMapper;
        }

        public Class<ErrorDecoder> getErrorDecoder() {
            return errorDecoder;
        }

        public void setErrorDecoder(Class<ErrorDecoder> errorDecoder) {
            this.errorDecoder = errorDecoder;
        }

        public List<Class<ReactiveHttpRequestInterceptor>> getRequestInterceptors() {
            return requestInterceptors;
        }

        public void setRequestInterceptors(List<Class<ReactiveHttpRequestInterceptor>> requestInterceptors) {
            this.requestInterceptors = requestInterceptors;
        }

        public Class<ReactiveLoggerListener> getLogger() {
            return logger;
        }

        public void setLogger(Class<ReactiveLoggerListener> logger) {
            this.logger = logger;
        }

        public Class<MicrometerReactiveLogger> getMetricsLogger() {
            return metricsLogger;
        }

        public void setMetricsLogger(Class<MicrometerReactiveLogger> metricsLogger) {
            this.metricsLogger = metricsLogger;
        }

        public Boolean getDecode404() {
            return decode404;
        }

        public void setDecode404(Boolean decode404) {
            this.decode404 = decode404;
        }

        public Class<Contract> getContract() {
            return contract;
        }

        public void setContract(Class<Contract> contract) {
            this.contract = contract;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReactiveFeignClientProperties that = (ReactiveFeignClientProperties) o;
            return Objects.equals(defaultToProperties, that.defaultToProperties) &&
                    Objects.equals(options, that.options) &&
                    Objects.equals(retryOnSame, that.retryOnSame) &&
                    Objects.equals(retryOnNext, that.retryOnNext) &&
                    Objects.equals(statusHandler, that.statusHandler) &&
                    Objects.equals(errorDecoder, that.errorDecoder) &&
                    Objects.equals(requestInterceptors, that.requestInterceptors) &&
                    Objects.equals(decode404, that.decode404) &&
                    Objects.equals(contract, that.contract) &&
                    Objects.equals(logger, that.logger) &&
                    Objects.equals(defaultQueryParameters, that.defaultQueryParameters) &&
                    Objects.equals(defaultRequestHeaders, that.defaultRequestHeaders);
        }

        @Override
        public int hashCode() {
            return Objects.hash(options, retryOnSame, retryOnNext,
                    statusHandler, errorDecoder, requestInterceptors,
                    decode404, contract, logger, defaultQueryParameters, defaultRequestHeaders);
        }

        public Map<String, List<String>> getDefaultRequestHeaders() {
            return defaultRequestHeaders;
        }

        public void setDefaultRequestHeaders(Map<String, List<String>> defaultRequestHeaders) {
            this.defaultRequestHeaders = defaultRequestHeaders;
        }

        public Map<String, List<String>> getDefaultQueryParameters() {
            return defaultQueryParameters;
        }

        public void setDefaultQueryParameters(Map<String, List<String>> defaultQueryParameters) {
            this.defaultQueryParameters = defaultQueryParameters;
        }
    }

    public static class RetryProperties {
        private Class<ReactiveRetryPolicy> policy;
        private Class<ReactiveRetryPolicy.Builder> builder;
        private Map args;

        public Class<ReactiveRetryPolicy> getPolicy() {
            return policy;
        }

        public void setPolicy(Class<ReactiveRetryPolicy> policy) {
            this.policy = policy;
        }

        public Class<ReactiveRetryPolicy.Builder> getBuilder() {
            return builder;
        }

        public void setBuilder(Class<ReactiveRetryPolicy.Builder> builder) {
            this.builder = builder;
        }

        public Map getArgs() {
            return args;
        }

        public void setArgs(Map args) {
            this.args = args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RetryProperties that = (RetryProperties) o;
            return Objects.equals(policy, that.policy) &&
                    Objects.equals(args, that.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(policy, args);
        }
    }

}

