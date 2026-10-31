package com.afterbooth.allocation.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "reservation_transition")
public class ReservationTransition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long reservationId;

  @Enumerated(EnumType.STRING)
  @Column
  private ReservationStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReservationStatus toStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransitionActor actor;

  @Column(nullable = false)
  private Instant at;

  protected ReservationTransition() {}

  private ReservationTransition(Long reservationId, TransitionActor actor, Instant at) {
    ReservationStatus fromStatus = null;
    ReservationStatus toStatus = ReservationStatus.HELD;
    Objects.requireNonNull(reservationId);
    Objects.requireNonNull(actor);
    Objects.requireNonNull(at);
    if ((actor != TransitionActor.AGENT && actor != TransitionActor.USER)) invalid();
    this.reservationId = reservationId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.actor = actor;
    this.at = at;
  }

  public static ReservationTransition initialHold(
      Long reservationId, TransitionActor actor, Instant at) {
    return new ReservationTransition(reservationId, actor, at);
  }

  public static ReservationTransition record(
      Long id, ReservationStatus from, ReservationStatus to, TransitionActor actor, Instant at) {
    ReservationTransition result = new ReservationTransition();
    result.reservationId = id;
    result.fromStatus = from;
    result.toStatus = to;
    result.actor = actor;
    result.at = at;
    return result;
  }

  private static void invalid() {
    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  public Long id() {
    return id;
  }

  public Long reservationId() {
    return reservationId;
  }

  public ReservationStatus fromStatus() {
    return fromStatus;
  }

  public ReservationStatus toStatus() {
    return toStatus;
  }

  public TransitionActor actor() {
    return actor;
  }

  public Instant at() {
    return at;
  }
}
