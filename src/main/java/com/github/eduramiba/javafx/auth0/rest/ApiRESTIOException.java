package com.github.eduramiba.javafx.auth0.rest;

import java.io.IOException;

public class ApiRESTIOException extends UserFriendlyIOException {

    public ApiRESTIOException(IOException cause) {
        super(cause);
    }

    @Override
    public String getFriendlyMessage() {
        Throwable e = getCause();
        return "Unexpected problem when contacting Application server REST API. Please make sure that your internet connection is working correctly."
                + " Also the service might be down at the moment. Please try again later. Problem: [" + e.getClass().getName() + "]: " + e.getMessage();
    }

    @Override
    public Object getSource() {
        return null;
    }
}
