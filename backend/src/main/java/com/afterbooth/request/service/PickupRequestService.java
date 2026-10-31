package com.afterbooth.request.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.request.domain.PickupRequest;
import com.afterbooth.request.domain.RequestFailure;
import com.afterbooth.request.dto.CreatePickupRequest;
import com.afterbooth.request.dto.PickupRequestResponse;
import com.afterbooth.request.repository.PickupRequestRepository;
import com.afterbooth.request.repository.RequestIdempotency;
import com.afterbooth.request.repository.RequestReferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PickupRequestService {
  private final PickupRequestRepository repository;
  private final RequestReferences references;
  private final RequestIdempotency idempotency;
  private final Clock clock;
  private final JsonMapper json;

  public PickupRequestService(
      PickupRequestRepository repository,
      RequestReferences references,
      RequestIdempotency idempotency,
      Clock clock,
      JsonMapper json) {
    this.repository = repository;
    this.references = references;
    this.idempotency = idempotency;
    this.clock = clock;
    this.json = json;
  }

  @Transactional
  public PickupRequestResponse create(UUID key, String body) {
    if (key == null) throw RequestFailure.invalid();
    CreatePickupRequest input = CreatePickupRequest.parse(json, body);
    String saved = idempotency.begin(key, fingerprint(body), clock.instant());
    if (saved != null) return json.readValue(saved, PickupRequestResponse.class);
    if (!references.organizationExists(input.organizationId())) throw RequestFailure.invalid();
    PickupRequest entity =
        PickupRequest.create(
            input.organizationId(),
            input.category(),
            input.quantity(),
            input.vehicleType(),
            input.pickupDeadline() == null
                ? null
                : input.pickupDeadline().toInstant().truncatedTo(ChronoUnit.MICROS),
            input.purpose(),
            input.transcript(),
            clock.instant().truncatedTo(ChronoUnit.MICROS));
    PickupRequestResponse response = PickupRequestResponse.from(repository.saveAndFlush(entity));
    idempotency.complete(key, json.writeValueAsString(response));
    return response;
  }

  @Transactional(readOnly = true)
  public PickupRequestResponse get(Long id) {
    return PickupRequestResponse.from(
        repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  private static String fingerprint(String body) {
    try {
      return "POST /api/pickup-requests "
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(body.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
