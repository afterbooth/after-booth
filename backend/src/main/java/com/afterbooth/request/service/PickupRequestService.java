package com.afterbooth.request.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.request.domain.PickupRequest;
import com.afterbooth.request.dto.CreatePickupRequest;
import com.afterbooth.request.dto.PickupRequestResponse;
import com.afterbooth.request.repository.PickupRequestRepository;
import com.afterbooth.request.repository.RequestReferences;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PickupRequestService {
  private final PickupRequestRepository repository;
  private final RequestReferences references;
  private final Clock clock;
  private final JsonMapper json;

  public PickupRequestService(
      PickupRequestRepository repository,
      RequestReferences references,
      Clock clock,
      JsonMapper json) {
    this.repository = repository;
    this.references = references;
    this.clock = clock;
    this.json = json;
  }

  @Transactional
  public PickupRequestResponse create(String body) {
    CreatePickupRequest input = CreatePickupRequest.parse(json, body);
    if (!references.organizationExists(input.organizationId()))
      throw new BusinessException(ErrorCode.INVALID_INPUT);
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
    return response;
  }

  @Transactional(readOnly = true)
  public PickupRequestResponse get(Long id) {
    return PickupRequestResponse.from(
        repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public PickupRequest lock(Long id) {
    return repository.lock(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
