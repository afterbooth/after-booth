package com.afterbooth.inventory.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.Condition;
import com.afterbooth.inventory.domain.Dimensions;
import com.afterbooth.inventory.domain.Event;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import com.afterbooth.inventory.dto.ItemResponse;
import com.afterbooth.inventory.repository.EventRepository;
import com.afterbooth.inventory.repository.ItemRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
  private final ItemRepository items;
  private final EventRepository events;

  public ItemService(ItemRepository items, EventRepository events) {
    this.items = items;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public void requireEvent(Long id) {
    findEvent(id);
  }

  @Transactional
  public List<ItemResponse> register(
      Long eventId,
      Long draftId,
      List<Registration> lines,
      Instant deadline,
      VehicleType vehicle,
      String transcript) {
    Event event = findEvent(eventId);
    Instant effective = event.intakePickupDeadline(deadline);
    return lines.stream()
        .map(
            line ->
                ItemResponse.from(
                    items.save(
                        Item.register(
                            eventId,
                            event.organizationId(),
                            draftId,
                            line.category(),
                            line.size(),
                            line.condition(),
                            line.quantity(),
                            effective,
                            vehicle,
                            transcript))))
        .toList();
  }

  private Event findEvent(Long id) {
    return events.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  public record Registration(
      ItemCategory category, int quantity, Dimensions size, Condition condition) {}
}
