package com.afterbooth.intake.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntakeDraftTest {
  @Test
  void startsProcessingAtEstimate() {
    var draft = IntakeDraft.start(1L, Instant.EPOCH);
    assertThat(draft.status()).isEqualTo(IntakeStatus.PROCESSING);
    assertThat(draft.stage()).isEqualTo(IntakeStage.ESTIMATE);
  }

  @Test
  void readyClearsProgressAndConfirmIsTerminal() {
    var draft = IntakeDraft.start(1L, Instant.EPOCH);
    draft.beginSegmentation();
    draft.ready();
    assertThat(draft.stage()).isNull();
    draft.confirm(List.of(2L), List.of(2L));
    assertThat(draft.status()).isEqualTo(IntakeStatus.CONFIRMED);
    assertThatThrownBy(() -> draft.confirm(List.of(2L), List.of(2L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).errorCode())
        .isEqualTo(ErrorCode.DRAFT_ALREADY_CONFIRMED);
  }

  @Test
  void cannotConfirmProcessingOrFailed() {
    var draft = IntakeDraft.start(1L, Instant.EPOCH);
    assertThatThrownBy(() -> draft.confirm(List.of(2L), List.of(2L)))
        .isInstanceOf(BusinessException.class);
    draft.fail("failure");
    assertThat(draft.stage()).isEqualTo(IntakeStage.ESTIMATE);
    assertThatThrownBy(() -> draft.confirm(List.of(2L), List.of(2L)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(draft::ready).isInstanceOf(BusinessException.class);
  }

  @Test
  void refusesForeignAndDuplicateAndEmptyLines() {
    var draft = IntakeDraft.start(1L, Instant.EPOCH);
    draft.ready();
    assertThatThrownBy(() -> draft.confirm(List.of(3L), List.of(2L)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> draft.confirm(List.of(2L, 2L), List.of(2L)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> draft.confirm(List.of(), List.of(2L)))
        .isInstanceOf(BusinessException.class);
    assertThat(draft.status()).isEqualTo(IntakeStatus.READY);
  }
}
