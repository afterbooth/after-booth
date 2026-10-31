package com.afterbooth.intake.domain;

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
import java.util.HashSet;
import java.util.List;
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

  public void beginSegmentation() {
    requireProcessing();
    if (stage != IntakeStage.ESTIMATE) throw new BusinessException(ErrorCode.DRAFT_NOT_READY);
    stage = IntakeStage.SEGMENT;
  }

  public void ready() {
    requireProcessing();
    status = IntakeStatus.READY;
    stage = null;
  }

  public void fail(String message) {
    requireProcessing();
    failureMessage = Objects.requireNonNull(message);
    status = IntakeStatus.FAILED;
  }

  public void confirm(List<Long> selectedIds, List<Long> ownIds) {
    if (status == IntakeStatus.CONFIRMED)
      throw new BusinessException(ErrorCode.DRAFT_ALREADY_CONFIRMED);
    if (status != IntakeStatus.READY) throw new BusinessException(ErrorCode.DRAFT_NOT_READY);
    if (selectedIds.isEmpty()
        || selectedIds.size() != new HashSet<>(selectedIds).size()
        || !ownIds.containsAll(selectedIds)) throw new BusinessException(ErrorCode.INVALID_INPUT);
    status = IntakeStatus.CONFIRMED;
  }

  private void requireProcessing() {
    if (status != IntakeStatus.PROCESSING) throw new BusinessException(ErrorCode.DRAFT_NOT_READY);
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
