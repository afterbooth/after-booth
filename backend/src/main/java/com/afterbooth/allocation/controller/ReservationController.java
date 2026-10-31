package com.afterbooth.allocation.controller;

import com.afterbooth.allocation.dto.HoldItemRequest;
import com.afterbooth.allocation.dto.ReservationResponse;
import com.afterbooth.allocation.service.ReservationService;
import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.error.ErrorResponse;
import com.afterbooth.global.idempotency.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/reservations")
class ReservationController {
  private final ReservationService reservations;
  private final IdempotencyService idempotency;
  private final JsonMapper json;
  private final Validator validator;

  ReservationController(
      ReservationService reservations,
      IdempotencyService idempotency,
      JsonMapper json,
      Validator validator) {
    this.reservations = reservations;
    this.idempotency = idempotency;
    this.json = json;
    this.validator = validator;
  }

  @GetMapping
  List<ReservationResponse> list(
      @RequestParam(required = false) Long requestId,
      @RequestParam(required = false) Long eventId) {
    return reservations.list(requestId, eventId);
  }

  @GetMapping("/{id}")
  ReservationResponse get(@PathVariable Long id) {
    return reservations.get(id);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> hold(
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody byte[] body,
      HttpServletRequest http) {
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        body,
        201,
        () -> {
          HoldItemRequest request;
          try {
            request = json.readValue(body, HoldItemRequest.class);
          } catch (RuntimeException error) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
          }
          if (request == null || !validator.validate(request).isEmpty())
            throw new BusinessException(ErrorCode.INVALID_INPUT);
          return reservations.hold(request);
        });
  }

  @PostMapping("/{id}/confirm")
  ResponseEntity<String> confirm(
      @PathVariable Long id,
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody(required = false) byte[] body,
      HttpServletRequest http) {
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        body == null ? new byte[0] : body,
        200,
        () -> {
          var result = reservations.confirm(id);
          return result.error() == null
              ? new IdempotencyService.Outcome(200, result.response())
              : new IdempotencyService.Outcome(
                  result.error().status().value(),
                  new ErrorResponse(result.error().name(), result.error().message()));
        });
  }

  @PostMapping("/{id}/cancel")
  ResponseEntity<String> cancel(
      @PathVariable Long id,
      @RequestHeader("Idempotency-Key") UUID key,
      @RequestBody(required = false) byte[] body,
      HttpServletRequest http) {
    return idempotency.execute(
        key,
        http.getMethod(),
        http.getRequestURI(),
        body == null ? new byte[0] : body,
        200,
        () -> reservations.cancel(id));
  }
}
