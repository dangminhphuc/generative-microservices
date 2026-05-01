package com.fintech.common.exception;

public class AccessDeniedException extends BaseException {

    public AccessDeniedException(String message) {
        super(message, "ACCESS_DENIED");
    }

    public AccessDeniedException() {
        super("Access denied", "ACCESS_DENIED");
    }
}
