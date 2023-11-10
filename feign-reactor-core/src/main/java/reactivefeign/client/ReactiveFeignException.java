package reactivefeign.client;

public class ReactiveFeignException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Error while running request: %s\n%s";

    private final ReactiveHttpRequest request;

    public ReactiveFeignException(Throwable cause, ReactiveHttpRequest request) {
        super(String.format(MESSAGE_PATTERN, request, cause.getMessage()), cause);
        this.request = request;
    }

    public ReactiveHttpRequest getRequest() {
        return request;
    }

}
