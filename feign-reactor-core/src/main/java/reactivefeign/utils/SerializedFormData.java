package reactivefeign.utils;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class SerializedFormData implements Publisher<Object> {

    private final String formDataString;
    private final ByteBuffer formData;

    private final Consumer<Object> logger;

    public SerializedFormData(String formDataString, ByteBuffer formData) {
        this(formDataString, formData, null);
    }

    private SerializedFormData(String formDataString, ByteBuffer formData, Consumer<Object> logger) {
        this.formDataString = formDataString;
        this.formData = formData;
        this.logger = logger;
    }

    public ByteBuffer getFormData() {
        if(logger != null) {
            logger.accept(this);
        }
        return formData;
    }

    public String getFormDataString() {
        if(logger != null) {
            logger.accept(this);
        }
        return formDataString;
    }

    @Override
    public void subscribe(Subscriber<? super Object> s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return formDataString;
    }

    public SerializedFormData logged(Consumer<Object> logger){
        return new SerializedFormData(formDataString, formData, logger);
    }
}

