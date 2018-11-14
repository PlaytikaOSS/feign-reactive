package reactivefeign.jetty.jetty;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.eclipse.jetty.reactive.client.internal.AbstractSingleProcessor;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fork of {@link org.eclipse.jetty.reactive.client.internal.QueuedSinglePublisher}
 * that waits for https://github.com/jetty-project/jetty-reactive-httpclient/issues fix
 * @param <T>
 *
 *
 * A Publisher that listens for response events.
 * When this Publisher is demanded data, it first sends the request and produces no data.
 * When the response arrives, the application is invoked and an application Publisher is
 * returned to this implementation.
 * Any further data demand to this Publisher is forwarded to the application Publisher.
 * In turn, the application Publisher produces data that is forwarded to the subscriber
 * of this Publisher.
 */
public class ResponseListenerPublisher<T> extends AbstractSingleProcessor<T, T> implements Response.Listener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final QueuedSinglePublisher<ContentChunk> content = new QueuedSinglePublisher<>();
    private final ReactiveRequest request;
    private final BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<T>> contentFn;
    private boolean requestSent;

    public ResponseListenerPublisher(ReactiveRequest request, BiFunction<ReactiveResponse, Publisher<ContentChunk>, Publisher<T>> contentFn) {
        this.request = request;
        this.contentFn = contentFn;
    }

    @Override
    public void onBegin(Response response) {
    }

    @Override
    public boolean onHeader(Response response, HttpField field) {
        return true;
    }

    @Override
    public void onHeaders(Response response) {
        if (logger.isDebugEnabled()) {
            logger.debug("received response headers {}", response);
        }
        Publisher<T> publisher = contentFn.apply(request.getReactiveResponse(), content);
        publisher.subscribe(this);
        content.start();
    }

    @Override
    public void onContent(Response response, ByteBuffer buffer, Callback callback) {
        if (logger.isDebugEnabled()) {
            logger.debug("received response chunk {} {}", response, BufferUtil.toSummaryString(buffer));
        }
        content.offer(new ContentChunk(buffer, callback));
    }

    @Override
    public void onContent(Response response, ByteBuffer content) {
    }

    @Override
    public void onSuccess(Response response) {
        if (logger.isDebugEnabled()) {
            logger.debug("response complete {}", response);
        }
    }

    @Override
    public void onFailure(Response response, Throwable failure) {
        if (logger.isDebugEnabled()) {
            logger.debug("response failure " + response, failure);
        }
    }

    @Override
    public void onComplete(Result result) {
        if (result.isSucceeded()) {
            if (!content.complete()) {
                onComplete();
            }
        } else {
            Throwable failure = result.getFailure();
            if (!content.fail(failure)) {
                onError(failure);
            }
        }
    }

    @Override
    protected void onRequest(Subscriber<? super T> subscriber, long n) {
        boolean send;
        synchronized (this) {
            send = !requestSent;
            requestSent = true;
        }
        if (send) {
            send();
        }
        super.onRequest(subscriber, n);
    }

    @Override
    public void onNext(T t) {
        downStreamOnNext(t);
    }

    private void send() {
        if (logger.isDebugEnabled()) {
            logger.debug("sending request {}", request);
        }
        request.getRequest().send(this);
    }

    @Override
    public String toString() {
        return String.format("%s@%x[%s]", getClass().getSimpleName(), hashCode(), request);
    }
}

