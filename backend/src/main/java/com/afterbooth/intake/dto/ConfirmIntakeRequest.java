package com.afterbooth.intake.dto;

import com.afterbooth.inventory.domain.Condition;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;

public record ConfirmIntakeRequest(
    @NotEmpty List<@NotNull @Valid ConfirmLine> lines,
    OffsetDateTime pickupDeadline,
    VehicleType vehicleRequirement,
    String conditionsTranscript) {
  public record ConfirmLine(
      @NotNull Long lineId,
      @NotNull ItemCategory category,
      @Positive int quantity,
      @NotNull @Valid SizeCm sizeCm,
      @NotNull Condition condition) {}
}
