package com.afterbooth.inventory.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

// TEMPLATE: entity with behavior. Copy, rename, delete what you do not need.
// - protected no-arg constructor for JPA, static factory for callers
// - no setters: state changes only through methods that enforce rules
// - other aggregates are referenced by ID
@Entity
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ItemCategory category;

  @Column(nullable = false)
  private int totalQty;

  @Column(nullable = false)
  private int availableQty;

  @Column(nullable = false)
  private Instant pickupDeadline;

  protected Item() {}

  private Item(Long organizationId, ItemCategory category, int qty, Instant pickupDeadline) {
    if (qty <= 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    this.organizationId = organizationId;
    this.category = category;
    this.totalQty = qty;
    this.availableQty = qty;
    this.pickupDeadline = pickupDeadline;
  }

  public static Item register(
      Long organizationId, ItemCategory category, int qty, Instant pickupDeadline) {
    return new Item(organizationId, category, qty, pickupDeadline);
  }

  public boolean isPickupClosed(Instant now) {
    return !now.isBefore(pickupDeadline);
  }

  public Long id() {
    return id;
  }

  public ItemCategory category() {
    return category;
  }

  public int availableQty() {
    return availableQty;
  }
}
