package com.afterbooth.intake.controller;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.idempotency.IdempotencyService;
import com.afterbooth.intake.dto.ConfirmIntakeRequest;
import com.afterbooth.intake.dto.IntakeDraftResponse;
import com.afterbooth.intake.service.IntakeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/intake/drafts")
class IntakeController {
  private final IntakeService service;
  private final IdempotencyService idempotency;
  private final JsonMapper json;
  private final Validator validator;

  IntakeController(
      IntakeService service, IdempotencyService idempotency, JsonMapper json, Validator validator) {
    this.service = service;
    this.idempotency = idempotency;
    this.json = json;
    this.validator = validator;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<String> create(
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestParam Long eventId,
      @RequestPart MultipartFile photo,
      HttpServletRequest http)
      throws IOException {
    byte[] bytes = photo.getBytes();
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        bytes,
        202,
        () -> service.create(eventId, bytes, photo.getContentType()));
  }

  @GetMapping("/{draftId}")
  IntakeDraftResponse get(@PathVariable Long draftId) {
    return service.get(draftId);
  }

  @PostMapping(value = "/{draftId}/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> confirm(
      @RequestHeader("Idempotency-Key") UUID key,
      @PathVariable Long draftId,
      @RequestBody byte[] body,
      HttpServletRequest http) {
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        body,
        201,
        () -> {
          ConfirmIntakeRequest request;
          try {
            request = json.readValue(body, ConfirmIntakeRequest.class);
          } catch (RuntimeException error) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
          }
          if (request == null || !validator.validate(request).isEmpty())
            throw new BusinessException(ErrorCode.INVALID_INPUT);
          return service.confirm(draftId, request);
        });
  }
}
