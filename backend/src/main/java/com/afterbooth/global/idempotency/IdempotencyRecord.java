package com.afterbooth.global.idempotency;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {
  @Id private UUID idempotencyKey;

  @Column(nullable = false)
  private String fingerprint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IdempotencyState state;

  @Column private Integer responseStatus;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = true, columnDefinition = "jsonb")
  private Map<String, Object> responseBody;

  @Column(nullable = false)
  private Instant createdAt;

  protected IdempotencyRecord() {}

  private IdempotencyRecord(UUID idempotencyKey, String fingerprint, Instant createdAt) {
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
    IdempotencyState state = IdempotencyState.IN_PROGRESS;
    Integer responseStatus = null;
    Map<String, Object> responseBody = null;
    Objects.requireNonNull(fingerprint);
    Objects.requireNonNull(state);
    Objects.requireNonNull(createdAt);
    if (fingerprint == null || fingerprint.isBlank()) invalid();
    this.fingerprint = fingerprint;
    this.state = state;
    this.responseStatus = responseStatus;
    this.responseBody = responseBody;
    this.createdAt = createdAt;
  }

  public static IdempotencyRecord begin(
      UUID idempotencyKey, String fingerprint, Instant createdAt) {
    return new IdempotencyRecord(idempotencyKey, fingerprint, createdAt);
  }

  private static void invalid() {
    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  public UUID idempotencyKey() {
    return idempotencyKey;
  }

  public String fingerprint() {
    return fingerprint;
  }

  public IdempotencyState state() {
    return state;
  }

  public Integer responseStatus() {
    return responseStatus;
  }

  public Map<String, Object> responseBody() {
    return responseBody;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
