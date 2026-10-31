package com.afterbooth.intake.pipeline;

import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.inventory.domain.Condition;
import java.math.BigDecimal;
import java.util.List;

public record EstimatedLine(
    DraftCategory category,
    int count,
    List<BoundingBox> boxes,
    Integer widthCm,
    Integer depthCm,
    Integer heightCm,
    Condition condition,
    BigDecimal confidence,
    boolean needsReview) {
  public EstimatedLine {
    boxes = List.copyOf(boxes);
  }

  public EstimatedLine samFailed() {
    return segmented(boxes, true);
  }

  public EstimatedLine segmented(List<BoundingBox> segmentedBoxes, boolean incomplete) {
    return new EstimatedLine(
        category,
        count,
        segmentedBoxes,
        widthCm,
        depthCm,
        heightCm,
        condition,
        confidence,
        needsReview || incomplete);
  }
}
