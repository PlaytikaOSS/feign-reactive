package reactivefeign.retry;

import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class BasicReactiveRetryPolicyTest {

  @Mock
  private Clock clock;

  @Test
  public void shouldRetry() {

    BasicReactiveRetryPolicy retryPolicy = new BasicReactiveRetryPolicy.Builder()
            .setMaxRetries(1)
            .build();


    long retryDelay = retryPolicy.retryDelay(new RuntimeException("error msg"), 1);
    assertThat(retryDelay).isEqualTo(0);

    retryDelay = retryPolicy.retryDelay(new RuntimeException("error msg"), 2);
    assertThat(retryDelay).isEqualTo(-1);
  }

  @Test
  public void shouldUseBackoffToDelay() {
    long backoff = 20;

    BasicReactiveRetryPolicy retryPolicy = new BasicReactiveRetryPolicy.Builder()
            .setMaxRetries(2).setBackoffInMs(backoff)
            .build();

    long retryDelay = retryPolicy.retryDelay(new RuntimeException("error msg"), 1);
    assertThat(retryDelay).isEqualTo(backoff);
  }

  @Test
  public void shouldTakeIntoAccountRetryAfter() {
    long delay = 10;
    long backoff = 20;

    BasicReactiveRetryPolicy retryPolicy = new BasicReactiveRetryPolicy.Builder()
            .setMaxRetries(2).setBackoffInMs(backoff)
            .setClock(clock).build();

    long currentTime = 1234;

    when(clock.millis()).thenReturn(currentTime);


    Request request = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), new byte[0], Charset.defaultCharset());
    long retryDelay = retryPolicy.retryDelay(new RetryableException(-1, "error msg", Request.HttpMethod.GET,
            new Date(currentTime + delay), request), 1);
    assertThat(retryDelay).isEqualTo(delay);

    retryDelay = retryPolicy.retryDelay(new RetryableException(-1, "error msg", Request.HttpMethod.GET,
            (Long) null, request), 1);
    assertThat(retryDelay).isEqualTo(backoff);
  }

}
