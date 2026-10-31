package com.afterbooth.inventory.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.Condition;
import com.afterbooth.inventory.domain.Dimensions;
import com.afterbooth.inventory.domain.Event;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import com.afterbooth.inventory.dto.IntakeInventoryItem;
import com.afterbooth.inventory.repository.IntakeEventRepository;
import com.afterbooth.inventory.repository.IntakeInventoryRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeInventoryService {
  private final IntakeInventoryRepository items;
  private final IntakeEventRepository events;

  public IntakeInventoryService(IntakeInventoryRepository items, IntakeEventRepository events) {
    this.items = items;
    this.events = events;
  }

  @Transactional(readOnly = true)
  public void requireEvent(Long id) {
    findEvent(id);
  }

  @Transactional
  public List<IntakeInventoryItem> register(
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
                IntakeInventoryItem.from(
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
