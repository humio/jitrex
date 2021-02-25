package com.humio.jitrex;

public class IllegalRegexException extends IllegalArgumentException {

    private final BadRegexCause reason;

    public enum BadRegexCause {
        REGEX_TOO_LONG,
        GENERATED_CLASS_INVALID
    }

    public IllegalRegexException(BadRegexCause reason, String message) {
        super(message);
        this.reason = reason;
    }

    public IllegalRegexException(BadRegexCause reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public BadRegexCause getReason() {
        return this.reason;
    }

}
