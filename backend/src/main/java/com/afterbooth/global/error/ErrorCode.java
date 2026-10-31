package com.afterbooth.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
  DRAFT_NOT_READY(HttpStatus.CONFLICT, "초안 처리가 끝난 뒤 확정해 주세요."),
  DRAFT_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정한 초안이에요."),
  IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "같은 요청을 처리 중이거나 요청 내용이 달라요."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없어요."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주세요.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String message() {
    return message;
  }
}
