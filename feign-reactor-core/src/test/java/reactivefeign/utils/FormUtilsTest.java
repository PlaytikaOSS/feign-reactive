package reactivefeign.utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FormUtilsTest {

    @Test
    public void shouldSerializeSimpleForm() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("key1", "value1");
        form.put("key2", "value2");

        SerializedFormData serialized = FormUtils.serializeForm(form, StandardCharsets.UTF_8);

        assertThat(serialized.getFormDataString()).isEqualTo("key1=value1&key2=value2");
    }

    @Test
    public void shouldSerializeFormWithCollection() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("key", Arrays.asList("value1", "value2"));

        SerializedFormData serialized = FormUtils.serializeForm(form, StandardCharsets.UTF_8);

        assertThat(serialized.getFormDataString()).isEqualTo("key=value1&key=value2");
    }

    @Test
    public void shouldSerializeFormWithSpecialCharacters() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("key ", "value&");

        SerializedFormData serialized = FormUtils.serializeForm(form, StandardCharsets.UTF_8);

        assertThat(serialized.getFormDataString()).isEqualTo("key+=value%26");
    }

    @Test
    public void shouldSerializeFormWithNullValue() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("key", null);

        SerializedFormData serialized = FormUtils.serializeForm(form, StandardCharsets.UTF_8);

        assertThat(serialized.getFormDataString()).isEqualTo("key");
    }

    @Test
    public void shouldSerializeFormWithDifferentCharset() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("key", "value");

        SerializedFormData serialized = FormUtils.serializeForm(form, StandardCharsets.ISO_8859_1);

        assertThat(serialized.getFormDataString()).isEqualTo("key=value");
        assertThat(serialized.getFormData()).isEqualTo(StandardCharsets.ISO_8859_1.encode("key=value"));
    }
}
