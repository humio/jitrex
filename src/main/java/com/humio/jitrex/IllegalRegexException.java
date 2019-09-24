package com.humio.jitrex;

public class IllegalRegexException extends IllegalArgumentException {

    private final BadRegexCause cause;

    public enum BadRegexCause {
        REGEX_TOO_LONG
    }

    public IllegalRegexException(BadRegexCause cause, String message) {
        super(message);
        this.cause = cause;
    }
}
