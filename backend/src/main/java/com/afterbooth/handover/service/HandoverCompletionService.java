package com.afterbooth.handover.service;

import com.afterbooth.allocation.domain.TransitionActor;
import com.afterbooth.allocation.dto.ReservationResponse;
import com.afterbooth.allocation.service.ReservationService;
import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.handover.domain.HandoverLog;
import com.afterbooth.handover.repository.HandoverLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandoverCompletionService {
  private final ReservationService reservations;
  private final HandoverLogRepository logs;

  public HandoverCompletionService(ReservationService reservations, HandoverLogRepository logs) {
    this.reservations = reservations;
    this.logs = logs;
  }

  @Transactional
  public ReservationResponse completeMatched(Long id) {
    ReservationResponse reservation = reservations.lockForHandover(id);
    HandoverLog log = newest(id);
    int accepted = log.requireMatched(reservation.quantity());
    return reservations.completeHandover(id, accepted, TransitionActor.HANDOVER);
  }

  @Transactional
  public ReservationResponse resolve(Long id, int accepted) {
    ReservationResponse reservation = reservations.lockForHandover(id);
    HandoverLog log = newest(id);
    log.resolve(accepted, reservation.quantity());
    logs.flush();
    return reservations.completeHandover(id, accepted, TransitionActor.USER);
  }

  private HandoverLog newest(Long id) {
    return logs.findFirstByReservationIdOrderByIdDesc(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TRANSITION));
  }
}
