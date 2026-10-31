package com.afterbooth.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.intake.domain.IntakeError;
import com.afterbooth.intake.domain.IntakeException;
import com.afterbooth.intake.repository.IntakeIdempotencyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class IntakeIdempotencyTest {
  private final IntakeIdempotencyRepository records = mock(IntakeIdempotencyRepository.class);
  private final JsonMapper json = JsonMapper.builder().build();
  private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
  private final UUID key = UUID.randomUUID();

  private IntakeIdempotency service() {
    when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    return new IntakeIdempotency(
        records, json, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), manager);
  }

  @Test
  void validationFailureIsStoredAndReplayedWithoutRunningActionAgain() {
    var service = service();
    when(records.claim(eq(key), eq("fp"), any())).thenReturn(true, false);
    Supplier<String> action = mock(Supplier.class);
    when(action.get()).thenThrow(new BusinessException(ErrorCode.INVALID_INPUT));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(BusinessException.class);
    var body = ArgumentCaptor.forClass(String.class);
    verify(records).finish(eq(key), eq(400), body.capture());
    when(records.find(key))
        .thenReturn(
            Optional.of(new IntakeIdempotencyRepository.Saved("fp", "DONE", body.getValue(), 400)));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).errorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
    verify(action, times(1)).get();
  }

  @Test
  void draftConflictIsStoredAndReplayedAsTheOriginalTypedError() {
    var service = service();
    when(records.claim(eq(key), eq("fp"), any())).thenReturn(true, false);
    Supplier<String> action = mock(Supplier.class);
    when(action.get()).thenThrow(new IntakeException(IntakeError.DRAFT_NOT_READY));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(IntakeException.class);
    var body = ArgumentCaptor.forClass(String.class);
    verify(records).finish(eq(key), eq(409), body.capture());
    when(records.find(key))
        .thenReturn(
            Optional.of(new IntakeIdempotencyRepository.Saved("fp", "DONE", body.getValue(), 409)));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(IntakeException.class)
        .extracting(e -> ((IntakeException) e).error())
        .isEqualTo(IntakeError.DRAFT_NOT_READY);
    verify(action, times(1)).get();
  }

  @Test
  void unexpectedFailureIsStoredAndReplayedWithoutRetry() {
    var service = service();
    when(records.claim(any(), any(), any())).thenReturn(true, false);
    Supplier<String> action = mock(Supplier.class);
    when(action.get()).thenThrow(new IllegalStateException("private infrastructure details"));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.INTERNAL_ERROR.message());
    var body = ArgumentCaptor.forClass(String.class);
    verify(records).finish(eq(key), eq(500), body.capture());
    assertThat(body.getValue()).doesNotContain("private infrastructure details");
    when(records.find(key))
        .thenReturn(
            Optional.of(new IntakeIdempotencyRepository.Saved("fp", "DONE", body.getValue(), 500)));
    assertThatThrownBy(
            () -> service.execute(key, "fp", 201, new TypeReference<String>() {}, action))
        .isInstanceOf(BusinessException.class)
        .hasMessage(ErrorCode.INTERNAL_ERROR.message());
    verify(action, times(1)).get();
  }

  @Test
  void savedArraysReplayAsArrays() {
    var service = service();
    when(records.claim(any(), any(), any())).thenReturn(false);
    when(records.find(key))
        .thenReturn(Optional.of(new IntakeIdempotencyRepository.Saved("fp", "DONE", "[1,2]", 201)));
    assertThat(
            service.execute(
                key,
                "fp",
                201,
                new TypeReference<List<Integer>>() {},
                () -> {
                  throw new AssertionError("must not run");
                }))
        .containsExactly(1, 2);
  }
}
