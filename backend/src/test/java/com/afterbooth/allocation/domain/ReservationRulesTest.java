package com.afterbooth.allocation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationRulesTest {
  private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

  private Reservation hold() {
    return Reservation.hold(1L, 1L, 2L, 1, NOW.plusSeconds(900), NOW.plusSeconds(3600), NOW);
  }

  @Test
  void confirmIsAllowedStrictlyBeforeHoldExpiry() {
    assertThat(hold().confirm(NOW.plusSeconds(899))).isEqualTo(ReservationStatus.CONFIRMED);
    assertThatThrownBy(() -> hold().confirm(NOW.plusSeconds(900)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.HOLD_EXPIRED));
  }

  @Test
  void expiryStartsExactlyAtDeadlineAndHeldCanBeCancelled() {
    assertThat(hold().dueTransition(NOW.plusSeconds(899))).isNull();
    assertThat(hold().dueTransition(NOW.plusSeconds(900))).isEqualTo(ReservationStatus.EXPIRED);
    assertThat(hold().cancel()).isEqualTo(ReservationStatus.RELEASED);
  }
}
