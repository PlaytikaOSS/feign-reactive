package reactivefeign.resttemplate.client;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.util.Collections.singletonList;

public class SerializedFormMessageConverter implements HttpMessageConverter<ByteBuffer> {

    private static final List<MediaType> MEDIA_TYPES = singletonList(MediaType.APPLICATION_FORM_URLENCODED);

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return ByteBuffer.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return MEDIA_TYPES;
    }

    @Override
    public ByteBuffer read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(ByteBuffer formData, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        byte[] b = new byte[formData.remaining()];
        formData.get(b, 0, b.length);
        outputMessage.getBody().write(b);
    }
}
