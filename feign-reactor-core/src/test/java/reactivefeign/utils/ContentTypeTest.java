package reactivefeign.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeTest {

    @Test
    public void shouldParseContentTypeWoCharset() {
        ContentType contentType = ContentType.parse("application/json");
        assertThat(contentType.getMediaType()).isEqualTo("application/json");
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldParseContentTypeWithCharset() {
        ContentType contentType = ContentType.parse("application/json; charset=utf-8");
        assertThat(contentType.getMediaType()).isEqualTo("application/json");
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldParseContentTypeWithCharsetQuoted() {
        ContentType contentType = ContentType.parse("application/json; charset=\"UTF-8\"");
        assertThat(contentType.getMediaType()).isEqualTo("application/json");
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldParseContentTypeWithDifferentCharset() {
        ContentType contentType = ContentType.parse("text/plain; charset=iso-8859-1");
        assertThat(contentType.getMediaType()).isEqualTo("text/plain");
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void shouldParseContentTypeWithUnsupportedCharset() {
        ContentType contentType = ContentType.parse("application/json; charset=wrong");
        assertThat(contentType.getMediaType()).isEqualTo("application/json");
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
    }
}
