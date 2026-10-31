package com.afterbooth.allocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ReservationSchedulerTest {
  @Test
  void failureForOneReservationDoesNotSkipRemainingRowsOrLeakItsMessage(CapturedOutput output) {
    ReservationService reservations = mock(ReservationService.class);
    when(reservations.due()).thenReturn(List.of(10L, 20L, 30L));
    when(reservations.expire(20L))
        .thenThrow(new IllegalStateException("sensitive failure payload"));
    new ReservationScheduler(reservations).sweep();
    verify(reservations).expire(10L);
    verify(reservations).expire(20L);
    verify(reservations).expire(30L);
    assertThat(output.getOut())
        .contains("reservationId=20", "errorType=IllegalStateException")
        .doesNotContain("sensitive failure payload");
  }
}
