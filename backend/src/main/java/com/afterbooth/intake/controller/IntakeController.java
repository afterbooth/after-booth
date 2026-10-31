package com.afterbooth.intake.controller;

import com.afterbooth.intake.dto.ConfirmIntakeRequest;
import com.afterbooth.intake.dto.IntakeDraftResponse;
import com.afterbooth.intake.service.IntakeService;
import com.afterbooth.inventory.dto.ItemResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/intake/drafts")
class IntakeController {
  private final IntakeService service;

  IntakeController(IntakeService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  IntakeDraftResponse create(
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestParam Long eventId,
      @RequestPart MultipartFile photo)
      throws IOException {
    return service.create(key, eventId, photo.getBytes(), photo.getContentType());
  }

  @GetMapping("/{draftId}")
  IntakeDraftResponse get(@PathVariable Long draftId) {
    return service.get(draftId);
  }

  @PostMapping("/{draftId}/confirm")
  @ResponseStatus(HttpStatus.CREATED)
  List<ItemResponse> confirm(
      @RequestHeader("Idempotency-Key") UUID key,
      @PathVariable Long draftId,
      @Valid @RequestBody ConfirmIntakeRequest request) {
    return service.confirm(key, draftId, request);
  }
}
