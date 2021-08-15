package reactivefeign.spring.config.cloud2;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import reactivefeign.spring.config.ReactiveFeignCircuitBreakerCustomizer;

import java.time.Duration;

public class BasicAutoconfigurationTest {

    static final String MOCK_SERVER_PORT_PROPERTY = "mock.server.port";

    static final ReactiveFeignCircuitBreakerCustomizer<Resilience4JConfigBuilder, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration>
            CIRCUIT_BREAKER_TIMEOUT_DISABLED_CONFIGURATION_CUSTOMIZER
            = configBuilder -> configBuilder
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(100)).build());
}
