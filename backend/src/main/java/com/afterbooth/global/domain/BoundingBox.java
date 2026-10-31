package com.afterbooth.global.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;

public record BoundingBox(double x, double y, double width, double height) {
  public BoundingBox {
    if (!valid(x) || !valid(y) || !valid(width) || !valid(height))
      throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  private static boolean valid(double value) {
    return Double.isFinite(value) && value >= 0 && value <= 1;
  }
}
