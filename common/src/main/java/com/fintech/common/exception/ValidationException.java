package com.fintech.common.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends BaseException {

    private final Map<String, List<String>> fieldErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
