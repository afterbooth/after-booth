package com.afterbooth.intake.pipeline;

public record IntakePhoto(byte[] bytes, String mediaType) {
  public IntakePhoto {
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }
}
