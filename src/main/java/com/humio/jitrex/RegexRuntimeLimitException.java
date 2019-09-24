package com.humio.jitrex;


public class RegexRuntimeLimitException extends RuntimeException {
    private final int backtrackCount;

    public RegexRuntimeLimitException(String msg, int backtrackCount) {
        super(msg);
        this.backtrackCount = backtrackCount;
    }

    public int getBacktrackCount() {
        return backtrackCount;
    }
}
