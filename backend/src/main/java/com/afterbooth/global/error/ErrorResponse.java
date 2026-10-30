package com.afterbooth.global.error;

/** The Error schema from contract/openapi.yaml. */
public record ErrorResponse(String code, String message) {}
