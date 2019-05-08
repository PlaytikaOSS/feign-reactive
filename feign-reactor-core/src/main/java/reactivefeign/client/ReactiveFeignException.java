package reactivefeign.client;

public class ReactiveFeignException extends RuntimeException{

    private final ReactiveHttpRequest request;

    public ReactiveFeignException(Throwable cause, ReactiveHttpRequest request) {
        super(cause);
        this.request = request;
    }

    public ReactiveHttpRequest getRequest() {
        return request;
    }
}
