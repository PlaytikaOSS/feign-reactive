package reactivefeign.spring.config;

import feign.Contract;
import reactivefeign.ReactiveOptions;
import reactivefeign.client.log.ReactiveLoggerListener;
import reactivefeign.retry.ReactiveRetryPolicy;
import reactivefeign.client.ReactiveHttpRequestInterceptor;
import reactivefeign.client.statushandler.ReactiveStatusHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReactiveFeignClientProperties<O extends ReactiveOptions.Builder> {

    private boolean defaultToProperties = true;

    private String defaultConfig = "default";

    private Map<String, ReactiveFeignClientConfiguration<O>> config = new HashMap<>();

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

    public Map<String, ReactiveFeignClientConfiguration<O>> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ReactiveFeignClientConfiguration<O>> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactiveFeignClientProperties that = (ReactiveFeignClientProperties) o;
        return defaultToProperties == that.defaultToProperties &&
                Objects.equals(defaultConfig, that.defaultConfig) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultToProperties, defaultConfig, config);
    }

    public static class ReactiveFeignClientConfiguration<O extends ReactiveOptions.Builder> {

        private O options;

        private RetryConfiguration retry;

        private Class<ReactiveStatusHandler> statusHandler;

        private List<Class<ReactiveHttpRequestInterceptor>> requestInterceptors;

        private Class<ReactiveLoggerListener> logger;

        private Boolean decode404;

        private Class<Contract> contract;

        public O getOptions() {
            return options;
        }

        public void setOptions(O options) {
            this.options = options;
        }

        public RetryConfiguration getRetry() {
            return retry;
        }

        public void setRetry(RetryConfiguration retry) {
            this.retry = retry;
        }

        public Class<ReactiveStatusHandler> getStatusHandler() {
            return statusHandler;
        }

        public void setStatusHandler(Class<ReactiveStatusHandler> statusHandler) {
            this.statusHandler = statusHandler;
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
            ReactiveFeignClientConfiguration that = (ReactiveFeignClientConfiguration) o;
            return Objects.equals(options, that.options) &&
                    Objects.equals(retry, that.retry) &&
                    Objects.equals(statusHandler, that.statusHandler) &&
                    Objects.equals(requestInterceptors, that.requestInterceptors) &&
                    Objects.equals(decode404, that.decode404) &&
                    Objects.equals(contract, that.contract) &&
                    Objects.equals(logger, that.logger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(options, retry,
                    statusHandler, requestInterceptors, decode404, contract, logger);
        }
    }

    public static class RetryConfiguration {
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
            RetryConfiguration that = (RetryConfiguration) o;
            return Objects.equals(policy, that.policy) &&
                    Objects.equals(args, that.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(policy, args);
        }
    }

}

