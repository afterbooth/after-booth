package com.afterbooth.allocation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ReservationScheduler {
  private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);
  private final ReservationService reservations;

  public ReservationScheduler(ReservationService reservations) {
    this.reservations = reservations;
  }

  @Scheduled(fixedDelayString = "${afterbooth.reservation-sweep-ms:30000}")
  public void sweep() {
    for (Long id : reservations.due()) {
      try {
        reservations.expire(id);
      } catch (RuntimeException failure) {
        log.error(
            "Reservation sweep failed: reservationId={}, errorType={}",
            id,
            failure.getClass().getSimpleName());
      }
    }
  }
}
