package com.afterbooth.request.service;

import static org.assertj.core.api.Assertions.*;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.idempotency.IdempotencyService;
import com.afterbooth.request.dto.PickupRequestResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PickupRequestIntegrationTest.Database.class)
class PickupRequestIntegrationTest {
  @Autowired PickupRequestService service;
  @Autowired IdempotencyService idempotency;
  @Autowired JsonMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;
  private static final String BODY =
      "{\"organizationId\":2,\"category\":\"DISPLAY_RACK\",\"quantity\":2,\"vehicleType\":\"SEDAN\",\"purpose\":\"주말"
          + " 행사\",\"transcript\":\"진열대 두 개\"}";

  @Test
  void savesReadsAndReplaysTheSameRequest() {
    UUID key = UUID.randomUUID();
    long before = jdbc.queryForObject("select count(*) from pickup_request", Long.class);
    var response = create(key, BODY);
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    var created = json.readValue(response.getBody(), PickupRequestResponse.class);
    assertThat(created.id()).isPositive();
    assertThat(service.get(created.id())).isEqualTo(created);
    var replay = create(key, BODY);
    assertThat(replay.getStatusCode().value()).isEqualTo(201);
    assertThat(json.readValue(replay.getBody(), PickupRequestResponse.class)).isEqualTo(created);
    assertThat(jdbc.queryForObject("select count(*) from pickup_request", Long.class))
        .isEqualTo(before + 1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from idempotency_record where idempotency_key = ? and state ="
                    + " 'DONE'",
                Integer.class,
                key))
        .isEqualTo(1);
    assertThatThrownBy(() -> create(key, BODY.replace("\"quantity\":2", "\"quantity\":3")))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode().name()).isEqualTo("IDEMPOTENCY_CONFLICT"));
  }

  @Test
  void inFlightKeyConflictsWithoutWaiting() throws Exception {
    UUID key = UUID.randomUUID();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              jdbc.queryForObject(
                  "select pg_advisory_xact_lock(?)",
                  Object.class,
                  key.getMostSignificantBits() ^ key.getLeastSignificantBits());
              var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
              try {
                var call =
                    executor.submit(
                        () -> {
                          assertThatThrownBy(() -> create(key, BODY))
                              .isInstanceOfSatisfying(
                                  BusinessException.class,
                                  error ->
                                      assertThat(error.errorCode().name())
                                          .isEqualTo("IDEMPOTENCY_CONFLICT"));
                        });
                try {
                  call.get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                  throw new AssertionError(e);
                }
              } finally {
                executor.shutdownNow();
              }
            });
  }

  private ResponseEntity<String> create(UUID key, String body) {
    return idempotency.execute(
        key,
        "POST",
        "/api/pickup-requests",
        body.getBytes(StandardCharsets.UTF_8),
        201,
        () -> service.create(body));
  }

  @Test
  void committedInProgressRequestConflicts() {
    UUID key = UUID.randomUUID();
    jdbc.update(
        "insert into idempotency_record(idempotency_key,fingerprint,state,created_at) values"
            + " (?,?,'IN_PROGRESS',current_timestamp)",
        key,
        IdempotencyService.fingerprint(
            "POST", "/api/pickup-requests", BODY.getBytes(StandardCharsets.UTF_8)));
    assertThatThrownBy(() -> create(key, BODY))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Database {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
      return new PostgreSQLContainer("postgres:17-alpine");
    }
  }
}
