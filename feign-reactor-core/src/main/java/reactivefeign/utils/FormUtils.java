package reactivefeign.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonList;

public class FormUtils {

    public static SerializedFormData serializeForm(Map<String, ?> formData, Charset charset) {
        StringBuilder builder = new StringBuilder();
        formData.forEach((name, values) ->
                (values instanceof Collection ? (Collection<Object>)values : singletonList(values))
                        .forEach(value -> {
                            try {
                                if (builder.length() != 0) {
                                    builder.append('&');
                                }
                                builder.append(URLEncoder.encode(name, charset.name()));
                                if (value != null) {
                                    builder.append('=');
                                    builder.append(URLEncoder.encode(value.toString(), charset.name()));
                                }
                            }
                            catch (UnsupportedEncodingException ex) {
                                throw new IllegalStateException(ex);
                            }
                        }));

        String formDataString = builder.toString();
        return new SerializedFormData(formDataString, ByteBuffer.wrap(formDataString.getBytes(charset)));
    }
}
