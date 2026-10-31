package com.afterbooth.request.domain;

public final class RequestFailure extends RuntimeException {
  private final String code;
  private final int status;

  private RequestFailure(String code, int status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public static RequestFailure invalid() {
    return new RequestFailure("INVALID_INPUT", 400, "입력값을 확인해 주세요.");
  }

  public static RequestFailure unsupported() {
    return new RequestFailure("UNSUPPORTED_REQUEST", 415, "16비트 PCM WAV 파일을 사용해 주세요.");
  }

  public static RequestFailure unavailable() {
    return new RequestFailure("AI_UNAVAILABLE", 503, "음성이나 조건을 해석하지 못했어요. 잠시 후 다시 시도해 주세요.");
  }

  public static RequestFailure conflict() {
    return new RequestFailure("IDEMPOTENCY_CONFLICT", 409, "같은 요청이 처리 중이거나 요청 내용이 달라요.");
  }

  public String code() {
    return code;
  }

  public int status() {
    return status;
  }
}
