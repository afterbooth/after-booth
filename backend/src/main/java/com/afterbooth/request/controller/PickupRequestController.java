package com.afterbooth.request.controller;

import com.afterbooth.request.dto.PickupRequestResponse;
import com.afterbooth.request.service.PickupRequestService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickup-requests")
class PickupRequestController {
  private final PickupRequestService service;

  PickupRequestController(PickupRequestService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  PickupRequestResponse create(
      @RequestHeader("Idempotency-Key") UUID key, @RequestBody String body) {
    return service.create(key, body);
  }

  @GetMapping("/{requestId}")
  PickupRequestResponse get(@PathVariable Long requestId) {
    return service.get(requestId);
  }
}
