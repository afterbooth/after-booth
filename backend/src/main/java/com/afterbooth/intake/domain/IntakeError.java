package com.afterbooth.intake.domain;

import org.springframework.http.HttpStatus;

public enum IntakeError {
  DRAFT_NOT_READY("초안 처리가 끝난 뒤 확정해 주세요."),
  DRAFT_ALREADY_CONFIRMED("이미 확정한 초안이에요."),
  IDEMPOTENCY_CONFLICT("같은 요청을 처리 중이거나 요청 내용이 달라요.");
  private final String message;

  IntakeError(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }

  public HttpStatus status() {
    return HttpStatus.CONFLICT;
  }
}
