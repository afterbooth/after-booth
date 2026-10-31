package com.afterbooth.intake.dto;

import com.afterbooth.inventory.domain.Dimensions;
import jakarta.validation.constraints.Positive;

public record SizeCm(@Positive int width, @Positive int depth, @Positive int height) {
  public Dimensions dimensions() {
    return new Dimensions(width, depth, height);
  }

  public static SizeCm from(Dimensions size) {
    return new SizeCm(size.widthCm(), size.depthCm(), size.heightCm());
  }
}
