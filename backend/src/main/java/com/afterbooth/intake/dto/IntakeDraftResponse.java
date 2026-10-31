package com.afterbooth.intake.dto;

import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.intake.domain.IntakeDraft;
import com.afterbooth.intake.domain.IntakeDraftLine;
import com.afterbooth.intake.domain.IntakeStage;
import com.afterbooth.intake.domain.IntakeStatus;
import com.afterbooth.inventory.domain.Condition;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record IntakeDraftResponse(
    Long id,
    Long eventId,
    IntakeStatus status,
    IntakeStage stage,
    String failureMessage,
    List<Line> lines,
    OffsetDateTime createdAt) {
  public static IntakeDraftResponse from(IntakeDraft draft, List<IntakeDraftLine> lines) {
    return new IntakeDraftResponse(
        draft.id(),
        draft.eventId(),
        draft.status(),
        draft.stage(),
        draft.failureMessage(),
        lines.stream().map(Line::from).toList(),
        draft.createdAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
  }

  public record Line(
      Long lineId,
      DraftCategory category,
      String label,
      int count,
      List<BoundingBox> boxes,
      SizeCm sizeCm,
      Condition condition,
      BigDecimal confidence,
      boolean needsReview) {
    static Line from(IntakeDraftLine line) {
      return new Line(
          line.id(),
          line.category(),
          line.label(),
          line.count(),
          line.boxes(),
          line.widthCm() == null
              ? null
              : new SizeCm(line.widthCm(), line.depthCm(), line.heightCm()),
          line.condition(),
          line.confidence(),
          line.needsReview());
    }
  }
}
