package reactivefeign.spring.config;

import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;

import java.util.function.Consumer;

public interface ReactiveFeignCircuitBreakerCustomizer<CONFB extends ConfigBuilder<CONF>, CONF> extends Consumer<CONFB> {}
