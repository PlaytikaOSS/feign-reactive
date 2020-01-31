package reactivefeign.cloud2;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

import java.util.function.Function;

public interface ReactiveFeignCircuitBreakerFactory extends Function<String, ReactiveCircuitBreaker> {
}
