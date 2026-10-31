package com.afterbooth.inventory.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import jakarta.persistence.Embeddable;
import java.util.Arrays;

@Embeddable
public record Dimensions(int widthCm, int depthCm, int heightCm) {
  public Dimensions {
    if (widthCm <= 0 || depthCm <= 0 || heightCm <= 0)
      throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  public boolean fitsIn(Dimensions cargo) {
    int[] itemAxes = {widthCm, depthCm, heightCm};
    int[] cargoAxes = {cargo.widthCm(), cargo.depthCm(), cargo.heightCm()};
    Arrays.sort(itemAxes);
    Arrays.sort(cargoAxes);
    return itemAxes[0] <= cargoAxes[0]
        && itemAxes[1] <= cargoAxes[1]
        && itemAxes[2] <= cargoAxes[2];
  }
}
