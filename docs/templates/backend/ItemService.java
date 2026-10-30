package com.afterbooth.inventory.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.dto.ItemResponse;
import com.afterbooth.inventory.dto.RegisterItemRequest;
import com.afterbooth.inventory.repository.ItemRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TEMPLATE: service. Orchestrates one use case per method: load, call domain, save, map.
// Business decisions belong in the entity, not here.
@Service
public class ItemService {

  private final ItemRepository itemRepository;
  private final Clock clock;

  ItemService(ItemRepository itemRepository, Clock clock) {
    this.itemRepository = itemRepository;
    this.clock = clock;
  }

  @Transactional
  public ItemResponse register(Long organizationId, RegisterItemRequest request) {
    Item item =
        Item.register(organizationId, request.category(), request.qty(), request.pickupDeadline());
    return ItemResponse.from(itemRepository.save(item));
  }

  @Transactional
  public void decreaseAvailable(Long itemId, int qty) {
    if (qty <= 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    Item item = find(itemId);
    if (item.isPickupClosed(clock.instant())) {
      throw new BusinessException(ErrorCode.PICKUP_CLOSED);
    }
    if (itemRepository.decreaseAvailable(itemId, qty) == 0) {
      throw new BusinessException(ErrorCode.QTY_UNAVAILABLE);
    }
  }

  @Transactional(readOnly = true)
  public ItemResponse get(Long itemId) {
    return ItemResponse.from(find(itemId));
  }

  private Item find(Long itemId) {
    return itemRepository
        .findById(itemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
