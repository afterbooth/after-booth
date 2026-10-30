// TEMPLATE: DTOs are records, one per file in real code.
// Requests validate their own shape. Responses are built from entities with a static factory.

package com.afterbooth.inventory.dto;

import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.ItemCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record RegisterItemRequest(
    @NotNull ItemCategory category, @Positive int qty, @NotNull @Future Instant pickupDeadline) {}

public record ItemResponse(Long id, ItemCategory category, int availableQty) {

  public static ItemResponse from(Item item) {
    return new ItemResponse(item.id(), item.category(), item.availableQty());
  }
}
