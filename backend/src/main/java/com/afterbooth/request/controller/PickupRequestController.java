package com.afterbooth.request.controller;

import com.afterbooth.global.idempotency.IdempotencyService;
import com.afterbooth.request.dto.PickupRequestResponse;
import com.afterbooth.request.service.PickupRequestService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickup-requests")
class PickupRequestController {
  private final PickupRequestService service;
  private final IdempotencyService idempotency;

  PickupRequestController(PickupRequestService service, IdempotencyService idempotency) {
    this.service = service;
    this.idempotency = idempotency;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> create(
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody byte[] body,
      HttpServletRequest http) {
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        body,
        201,
        () -> service.create(new String(body, StandardCharsets.UTF_8)));
  }

  @GetMapping("/{requestId}")
  PickupRequestResponse get(@PathVariable Long requestId) {
    return service.get(requestId);
  }
}
