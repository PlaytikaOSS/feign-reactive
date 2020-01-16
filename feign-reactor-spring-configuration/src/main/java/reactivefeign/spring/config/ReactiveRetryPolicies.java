package reactivefeign.spring.config;

import reactivefeign.retry.ReactiveRetryPolicy;

public class ReactiveRetryPolicies {

    private final ReactiveRetryPolicy retryOnSame;
    private final ReactiveRetryPolicy retryOnNext;

    private ReactiveRetryPolicies(ReactiveRetryPolicy retryOnSame, ReactiveRetryPolicy retryOnNext) {
        this.retryOnSame = retryOnSame;
        this.retryOnNext = retryOnNext;
    }

    public ReactiveRetryPolicy getRetryOnSame() {
        return retryOnSame;
    }

    public ReactiveRetryPolicy getRetryOnNext() {
        return retryOnNext;
    }

    public static class Builder{
        private ReactiveRetryPolicy retryOnSame;
        private ReactiveRetryPolicy retryOnNext;

        public Builder retryOnSame(ReactiveRetryPolicy retryOnSame) {
            this.retryOnSame = retryOnSame;
            return this;
        }

        public Builder retryOnNext(ReactiveRetryPolicy retryOnNext) {
            this.retryOnNext = retryOnNext;
            return this;
        }

        public ReactiveRetryPolicies build(){
            return new ReactiveRetryPolicies(retryOnSame, retryOnNext);
        }
    }
}
