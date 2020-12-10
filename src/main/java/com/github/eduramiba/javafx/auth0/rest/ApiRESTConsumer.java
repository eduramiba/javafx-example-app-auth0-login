package com.github.eduramiba.javafx.auth0.rest;

import net.jodah.failsafe.RetryPolicy;

public interface ApiRESTConsumer extends AutoCloseable {

    /**
     * Sets both connection timeout and read timeout.
     *
     * @param timeoutMilliseconds
     */
    default void setTimeoutMillis(int timeoutMilliseconds) {
        setConnectionTimeoutMillis(timeoutMilliseconds);
        setReadTimeoutMillis(timeoutMilliseconds);
        setWriteTimeoutMillis(timeoutMilliseconds);
    }

    default void setConnectionTimeoutMillis(int timeoutMilliseconds) {
        //NOOP
    }

    default void setReadTimeoutMillis(int timeoutMilliseconds) {
        //NOOP
    }

    default void setWriteTimeoutMillis(int timeoutMilliseconds) {
        //NOOP
    }

    default void addRESTClientHeadersProvider(ApiRESTClientHeadersProvider headersProvider) {
        //NOOP
    }

    default void setRetryPolicy(RetryPolicy<?> retryPolicy) {
        //NOOP
    }

    @Override
    default void close() {
        //NOOP
    }
}
