package com.afterbooth.request.dto;

import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import com.afterbooth.request.domain.PickupRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PickupRequestResponse(
    Long id,
    Long organizationId,
    ItemCategory category,
    int quantity,
    VehicleType vehicleType,
    OffsetDateTime pickupDeadline,
    String purpose,
    String transcript,
    OffsetDateTime createdAt) {
  public PickupRequestResponse {
    pickupDeadline =
        pickupDeadline == null
            ? null
            : pickupDeadline.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    createdAt = createdAt.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
  }

  public static PickupRequestResponse from(PickupRequest request) {
    return new PickupRequestResponse(
        request.id(),
        request.organizationId(),
        request.category(),
        request.quantity(),
        request.vehicleType(),
        seoul(request.pickupDeadline()),
        request.purpose(),
        request.transcript(),
        seoul(request.createdAt()));
  }

  private static OffsetDateTime seoul(Instant instant) {
    return instant == null ? null : instant.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
  }
}
