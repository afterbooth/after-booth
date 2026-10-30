package com.afterbooth.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

// TEMPLATE: domain rule test. Plain JUnit, no Spring, runs in milliseconds.
class ItemTest {

  private static final Instant DEADLINE = Instant.parse("2026-11-01T11:00:00Z");

  @Test
  void registerRejectsNonPositiveQuantity() {
    assertThatThrownBy(() -> Item.register(1L, ItemCategory.DISPLAY_RACK, 0, DEADLINE))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void pickupClosesAtDeadline() {
    Item item = Item.register(1L, ItemCategory.DISPLAY_RACK, 4, DEADLINE);

    assertThat(item.isPickupClosed(DEADLINE.minusSeconds(1))).isFalse();
    assertThat(item.isPickupClosed(DEADLINE)).isTrue();
  }
}
