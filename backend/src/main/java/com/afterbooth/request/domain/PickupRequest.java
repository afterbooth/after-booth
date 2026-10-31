package com.afterbooth.request.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
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
@Table(name = "pickup_request")
public class PickupRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ItemCategory category;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VehicleType vehicleType;

  @Column private Instant pickupDeadline;

  @Column private String purpose;

  @Column private String transcript;

  @Column(nullable = false)
  private Instant createdAt;

  protected PickupRequest() {}

  private PickupRequest(
      Long organizationId,
      ItemCategory category,
      int quantity,
      VehicleType vehicleType,
      Instant pickupDeadline,
      String purpose,
      String transcript,
      Instant createdAt) {
    Objects.requireNonNull(organizationId);
    Objects.requireNonNull(category);
    Objects.requireNonNull(vehicleType);
    Objects.requireNonNull(createdAt);
    if (quantity <= 0) invalid();
    this.organizationId = organizationId;
    this.category = category;
    this.quantity = quantity;
    this.vehicleType = vehicleType;
    this.pickupDeadline = pickupDeadline;
    this.purpose = purpose;
    this.transcript = transcript;
    this.createdAt = createdAt;
  }

  public static PickupRequest create(
      Long organizationId,
      ItemCategory category,
      int quantity,
      VehicleType vehicleType,
      Instant pickupDeadline,
      String purpose,
      String transcript,
      Instant createdAt) {
    return new PickupRequest(
        organizationId,
        category,
        quantity,
        vehicleType,
        pickupDeadline,
        purpose,
        transcript,
        createdAt);
  }

  private static void invalid() {
    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }

  public Long id() {
    return id;
  }

  public Long organizationId() {
    return organizationId;
  }

  public ItemCategory category() {
    return category;
  }

  public int quantity() {
    return quantity;
  }

  public VehicleType vehicleType() {
    return vehicleType;
  }

  public Instant pickupDeadline() {
    return pickupDeadline;
  }

  public String purpose() {
    return purpose;
  }

  public String transcript() {
    return transcript;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
