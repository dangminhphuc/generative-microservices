package com.fintech.common.exception;

public class BusinessRuleException extends BaseException {

    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }
}
