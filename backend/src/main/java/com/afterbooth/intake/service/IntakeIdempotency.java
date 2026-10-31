package com.afterbooth.intake.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.error.ErrorResponse;
import com.afterbooth.intake.domain.IntakeError;
import com.afterbooth.intake.domain.IntakeException;
import com.afterbooth.intake.repository.IntakeIdempotencyRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IntakeIdempotency {
  private static final Logger log = LoggerFactory.getLogger(IntakeIdempotency.class);
  private final IntakeIdempotencyRepository records;
  private final JsonMapper json;
  private final Clock clock;
  private final TransactionTemplate tx;

  public IntakeIdempotency(
      IntakeIdempotencyRepository records,
      JsonMapper json,
      Clock clock,
      PlatformTransactionManager manager) {
    this.records = records;
    this.json = json;
    this.clock = clock;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public <T> T execute(
      UUID key, String fingerprint, int status, TypeReference<T> type, Supplier<T> action) {
    boolean claimed =
        Boolean.TRUE.equals(tx.execute(s -> records.claim(key, fingerprint, clock.instant())));
    if (!claimed) {
      var saved =
          records
              .find(key)
              .orElseThrow(() -> new IntakeException(IntakeError.IDEMPOTENCY_CONFLICT));
      if (!saved.fingerprint().equals(fingerprint) || !saved.state().equals("DONE"))
        throw new IntakeException(IntakeError.IDEMPOTENCY_CONFLICT);
      if (saved.status() != null && saved.status() >= 400) throw replayFailure(saved.body());
      return json.readValue(saved.body(), type);
    }
    try {
      return tx.execute(
          s -> {
            T response = action.get();
            records.finish(key, status, json.writeValueAsString(response));
            return response;
          });
    } catch (BusinessException e) {
      tx.executeWithoutResult(
          s ->
              records.finish(
                  key,
                  e.errorCode().status().value(),
                  json.writeValueAsString(
                      new ErrorResponse(e.errorCode().name(), e.getMessage()))));
      throw e;
    } catch (IntakeException e) {
      tx.executeWithoutResult(
          s ->
              records.finish(
                  key,
                  e.error().status().value(),
                  json.writeValueAsString(new ErrorResponse(e.error().name(), e.getMessage()))));
      throw e;
    } catch (RuntimeException e) {
      log.error("Intake operation failed", e);
      tx.executeWithoutResult(
          s ->
              records.finish(
                  key,
                  500,
                  json.writeValueAsString(
                      new ErrorResponse(
                          ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.message()))));
      BusinessException failure = new BusinessException(ErrorCode.INTERNAL_ERROR);
      failure.initCause(e);
      throw failure;
    }
  }

  private RuntimeException replayFailure(String body) {
    ErrorResponse error = json.readValue(body, ErrorResponse.class);
    return switch (error.code()) {
      case "DRAFT_NOT_READY", "DRAFT_ALREADY_CONFIRMED", "IDEMPOTENCY_CONFLICT" ->
          new IntakeException(IntakeError.valueOf(error.code()), error.message());
      default -> new BusinessException(ErrorCode.valueOf(error.code()), error.message());
    };
  }

  public String fingerprint(String path, Object body) {
    return "POST " + path + " " + hash(json.writeValueAsBytes(body));
  }

  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
