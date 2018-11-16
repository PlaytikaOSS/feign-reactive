package reactivefeign.jetty.client;

import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.internal.QueuedSinglePublisher;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.slf4j.LoggerFactory;
import reactivefeign.client.ReactiveHttpResponse;
import reactor.core.publisher.MonoSink;

import java.nio.ByteBuffer;

import static reactivefeign.jetty.utils.ProxyPostProcessor.postProcess;

public class JettyReactiveResponseListener implements Response.Listener{

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(JettyReactiveResponseListener.class);

    private final QueuedSinglePublisher<ContentChunk> content = new QueuedSinglePublisher<>();

    private final MonoSink<ReactiveHttpResponse> sink;
    private Class returnPublisherType;
    private Class returnActualClass;
    private JsonFactory jsonFactory;
    private ObjectReader objectReader;

    public JettyReactiveResponseListener(MonoSink<ReactiveHttpResponse> sink,
                                         Class returnPublisherType, Class returnActualClass,
                                         JsonFactory jsonFactory, ObjectReader objectReader) {
        this.sink = sink;
        this.returnPublisherType = returnPublisherType;
        this.returnActualClass = returnActualClass;
        this.jsonFactory = jsonFactory;
        this.objectReader = objectReader;
    }

    @Override
    public void onHeaders(Response response) {

        if (logger.isDebugEnabled()) {
            logger.debug("received response headers {}", response);
        }

        sink.success(new JettyReactiveHttpResponse(response,
                postProcess(content,
                        (contentChunk, throwable) -> {
                            if(throwable != null){
                                contentChunk.callback.failed(throwable);
                            } else {
                                contentChunk.callback.succeeded();
                            }
                        }),
                returnPublisherType, returnActualClass, jsonFactory, objectReader));
    }

    @Override
    public void onContent(Response response, ByteBuffer buffer, Callback callback) {
        if (logger.isDebugEnabled()) {
            logger.debug("received response chunk {} {}", response, BufferUtil.toSummaryString(buffer));
        }

        content.offer(new ContentChunk(buffer, callback));
    }

    @Override
    public void onComplete(Result result) {
        Response response = result.getResponse();
        if (result.isSucceeded()) {
            if (logger.isDebugEnabled()) {
                logger.debug("response complete {}", response);
            }

            content.complete();
        } else {
            Throwable failure = result.getFailure();
            if (logger.isDebugEnabled()) {
                logger.debug("response failure " + response, failure);
            }

            if (!content.fail(failure)) {
                sink.error(failure);
            }
        }
    }

    @Override
    public void onSuccess(Response response) {}

    @Override
    public void onFailure(Response response, Throwable failure) {}


    @Override
    public void onBegin(Response response) {}

    @Override
    public void onContent(Response response, ByteBuffer content) {}

    @Override
    public boolean onHeader(Response response, HttpField field) {
        return true;
    }

}
