package com.github.eduramiba.javafx.auth0.rest;

public abstract class UserFriendlyIOException extends RuntimeException implements UserFriendlyException {

    public UserFriendlyIOException() {
    }

    public UserFriendlyIOException(String message) {
        super(message);
    }

    public UserFriendlyIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserFriendlyIOException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getFriendlyMessage() {
        return getMessage();
    }
}
