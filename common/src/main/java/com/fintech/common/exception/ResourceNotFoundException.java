package com.fintech.common.exception;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("%s not found: %s".formatted(resourceType, identifier), "RESOURCE_NOT_FOUND");
    }
}
