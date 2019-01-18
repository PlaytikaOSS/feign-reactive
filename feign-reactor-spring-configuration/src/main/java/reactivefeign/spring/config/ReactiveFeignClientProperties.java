package reactivefeign.spring.config;

import feign.Contract;
import reactivefeign.ReactiveOptions;
import reactivefeign.ReactiveRetryPolicy;
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

        private Class<ReactiveRetryPolicy> retryPolicy;

        private Class<ReactiveStatusHandler> statusHandler;

        private List<Class<ReactiveHttpRequestInterceptor>> requestInterceptors;

        private Boolean decode404;

        private Class<Contract> contract;

        public O getOptions() {
            return options;
        }

        public void setOptions(O options) {
            this.options = options;
        }

        public Class<ReactiveRetryPolicy> getRetryPolicy() {
            return retryPolicy;
        }

        public void setRetryPolicy(Class<ReactiveRetryPolicy> retryPolicy) {
            this.retryPolicy = retryPolicy;
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
                    Objects.equals(retryPolicy, that.retryPolicy) &&
                    Objects.equals(statusHandler, that.statusHandler) &&
                    Objects.equals(requestInterceptors, that.requestInterceptors) &&
                    Objects.equals(decode404, that.decode404) &&
                    Objects.equals(contract, that.contract);
        }

        @Override
        public int hashCode() {
            return Objects.hash(options, retryPolicy,
                    statusHandler, requestInterceptors, decode404, contract);
        }
    }

}

