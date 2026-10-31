package com.afterbooth.inventory.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import jakarta.persistence.Embeddable;

@Embeddable
public record Dimensions(int widthCm, int depthCm, int heightCm) {
  public Dimensions {
    if (widthCm <= 0 || depthCm <= 0 || heightCm <= 0)
      throw new BusinessException(ErrorCode.INVALID_INPUT);
  }
}
