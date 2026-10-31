package com.afterbooth.allocation.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ReservationScheduler {
  private final ReservationService reservations;

  public ReservationScheduler(ReservationService reservations) {
    this.reservations = reservations;
  }

  @Scheduled(fixedDelayString = "${afterbooth.reservation-sweep-ms:30000}")
  public void sweep() {
    for (Long id : reservations.due()) reservations.expire(id);
  }
}
