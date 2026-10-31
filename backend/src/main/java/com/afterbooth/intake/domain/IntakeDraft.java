package com.afterbooth.intake.domain;

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
@Table(name = "intake_draft")
public class IntakeDraft {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IntakeStatus status;

  @Enumerated(EnumType.STRING)
  @Column
  private IntakeStage stage;

  @Column private String failureMessage;

  @Column(nullable = false)
  private Instant createdAt;

  protected IntakeDraft() {}

  private IntakeDraft(Long eventId, Instant createdAt) {
    Objects.requireNonNull(eventId);
    Objects.requireNonNull(createdAt);

    this.eventId = eventId;
    this.status = IntakeStatus.PROCESSING;
    this.stage = IntakeStage.ESTIMATE;
    this.createdAt = createdAt;
  }

  public static IntakeDraft start(Long eventId, Instant createdAt) {
    return new IntakeDraft(eventId, createdAt);
  }

  public Long id() {
    return id;
  }

  public Long eventId() {
    return eventId;
  }

  public IntakeStatus status() {
    return status;
  }

  public IntakeStage stage() {
    return stage;
  }

  public String failureMessage() {
    return failureMessage;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
