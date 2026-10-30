package com.afterbooth.global.error;

/** contract/openapi.yaml의 Error 스키마. */
public record ErrorResponse(String code, String message) {}
