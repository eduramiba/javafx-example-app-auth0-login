package com.github.eduramiba.javafx.auth0.rest;

/**
 * A kind of exception that has a user friendly message with possible solutions. Normally it's a controlled exception, meaning the program should not crash.
 */
public interface UserFriendlyException {

    String getFriendlyMessage();

    default boolean isControlled() {
        return true;
    }

    default String getUniqueCode() {
        return null;
    }

    default String getMessage() {
        return getFriendlyMessage();
    }

    /**
     * Returns the source of the problem, the wrong thing.
     * @return Null or something
     */
    default Object getSource() {
        return null;
    }

    default Throwable toThrowable() {
        return (Throwable) this;
    }
}
