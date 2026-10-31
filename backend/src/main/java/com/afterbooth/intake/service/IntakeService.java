package com.afterbooth.intake.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.intake.domain.IntakeDraft;
import com.afterbooth.intake.domain.IntakeDraftLine;
import com.afterbooth.intake.domain.IntakeStatus;
import com.afterbooth.intake.domain.IntakeUpload;
import com.afterbooth.intake.dto.ConfirmIntakeRequest;
import com.afterbooth.intake.dto.IntakeDraftResponse;
import com.afterbooth.intake.pipeline.IntakePhoto;
import com.afterbooth.intake.repository.IntakeDraftLineRepository;
import com.afterbooth.intake.repository.IntakeDraftRepository;
import com.afterbooth.inventory.dto.IntakeInventoryItem;
import com.afterbooth.inventory.service.IntakeInventoryService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

@Service
public class IntakeService {
  private final IntakeDraftRepository drafts;
  private final IntakeDraftLineRepository lines;
  private final IntakeInventoryService inventory;
  private final IntakeIdempotency idempotency;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public IntakeService(
      IntakeDraftRepository drafts,
      IntakeDraftLineRepository lines,
      IntakeInventoryService inventory,
      IntakeIdempotency idempotency,
      ApplicationEventPublisher events,
      Clock clock) {
    this.drafts = drafts;
    this.lines = lines;
    this.inventory = inventory;
    this.idempotency = idempotency;
    this.events = events;
    this.clock = clock;
  }

  public IntakeDraftResponse create(UUID key, Long eventId, byte[] bytes, String mediaType) {
    IntakeUpload upload = new IntakeUpload(bytes, mediaType);
    String fingerprint =
        idempotency.fingerprint(
            "/api/intake/drafts", List.of(eventId, mediaType, IntakeIdempotency.hash(bytes)));
    return idempotency.execute(
        key,
        fingerprint,
        202,
        new TypeReference<IntakeDraftResponse>() {},
        () -> {
          inventory.requireEvent(eventId);
          IntakeDraft draft = drafts.saveAndFlush(IntakeDraft.start(eventId, clock.instant()));
          IntakeDraftResponse response = IntakeDraftResponse.from(draft, List.of());
          events.publishEvent(
              new IntakeProcessor.Submitted(
                  draft.id(), new IntakePhoto(upload.bytes(), upload.mediaType())));
          return response;
        });
  }

  @Transactional(readOnly = true)
  public IntakeDraftResponse get(Long id) {
    IntakeDraft draft = find(id);
    return IntakeDraftResponse.from(
        draft,
        draft.status() == IntakeStatus.PROCESSING ? List.of() : lines.findByDraftIdOrderById(id));
  }

  public List<IntakeInventoryItem> confirm(UUID key, Long id, ConfirmIntakeRequest request) {
    String fingerprint = idempotency.fingerprint("/api/intake/drafts/" + id + "/confirm", request);
    return idempotency.execute(
        key,
        fingerprint,
        201,
        new TypeReference<List<IntakeInventoryItem>>() {},
        () -> {
          IntakeDraft draft =
              drafts.findLocked(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
          draft.confirm(
              request.lines().stream().map(ConfirmIntakeRequest.ConfirmLine::lineId).toList(),
              lines.findByDraftIdOrderById(id).stream().map(IntakeDraftLine::id).toList());
          return inventory.register(
              draft.eventId(),
              id,
              request.lines().stream()
                  .map(
                      line ->
                          new IntakeInventoryService.Registration(
                              line.category(),
                              line.quantity(),
                              line.sizeCm().dimensions(),
                              line.condition()))
                  .toList(),
              request.pickupDeadline() == null ? null : request.pickupDeadline().toInstant(),
              request.vehicleRequirement(),
              request.conditionsTranscript());
        });
  }

  private IntakeDraft find(Long id) {
    return drafts.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
