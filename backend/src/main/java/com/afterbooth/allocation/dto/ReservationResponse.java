package com.afterbooth.allocation.dto;

import com.afterbooth.allocation.domain.Reservation;
import com.afterbooth.allocation.domain.ReservationStatus;
import com.afterbooth.allocation.domain.TransitionActor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ReservationResponse(
    Long id,
    Long requestId,
    Long organizationId,
    Long itemId,
    String itemLabel,
    int quantity,
    Integer handedOverQty,
    ReservationStatus status,
    OffsetDateTime holdExpiresAt,
    OffsetDateTime pickupDeadline,
    OffsetDateTime createdAt,
    List<Transition> transitions) {
  public record Transition(
      ReservationStatus from, ReservationStatus to, OffsetDateTime at, TransitionActor actor) {}

  public static OffsetDateTime time(Instant value) {
    return value.atOffset(ZoneOffset.ofHours(9));
  }

  public static ReservationResponse from(
      Reservation r, String label, List<Transition> transitions) {
    return new ReservationResponse(
        r.id(),
        r.requestId(),
        r.organizationId(),
        r.itemId(),
        label,
        r.quantity(),
        r.handedOverQty(),
        r.status(),
        time(r.holdExpiresAt()),
        time(r.pickupDeadline()),
        time(r.createdAt()),
        transitions);
  }
}
