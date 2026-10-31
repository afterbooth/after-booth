package com.afterbooth.request.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.request.domain.PickupRequest;
import com.afterbooth.request.repository.PickupRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupRequestService {
  private final PickupRequestRepository repository;

  public PickupRequestService(PickupRequestRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public PickupRequest lock(Long id) {
    return repository.lock(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
