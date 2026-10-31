package com.afterbooth.intake.domain;

public class IntakeException extends RuntimeException {
  private final IntakeError error;

  public IntakeException(IntakeError error) {
    this(error, error.message());
  }

  public IntakeException(IntakeError error, String message) {
    super(message);
    this.error = error;
  }

  public IntakeError error() {
    return error;
  }
}
