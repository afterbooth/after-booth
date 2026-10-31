package com.afterbooth.request.service;

import static org.assertj.core.api.Assertions.*;

import com.afterbooth.request.domain.RequestFailure;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PickupRequestIntegrationTest.Database.class)
class PickupRequestIntegrationTest {
  @Autowired PickupRequestService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;
  private static final String BODY =
      "{\"organizationId\":2,\"category\":\"DISPLAY_RACK\",\"quantity\":2,\"vehicleType\":\"SEDAN\",\"purpose\":\"주말 행사\",\"transcript\":\"진열대 두 개\"}";

  @Test
  void savesReadsAndReplaysTheSameRequest() {
    UUID key = UUID.randomUUID();
    var created = service.create(key, BODY);
    assertThat(created.id()).isPositive();
    assertThat(service.get(created.id())).isEqualTo(created);
    assertThat(service.create(key, BODY)).isEqualTo(created);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from idempotency_record where idempotency_key = ? and state = 'DONE'",
                Integer.class,
                key))
        .isEqualTo(1);
    assertThatThrownBy(() -> service.create(key, BODY.replace("\"quantity\":2", "\"quantity\":3")))
        .isInstanceOfSatisfying(
            RequestFailure.class,
            error -> assertThat(error.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
  }

  @Test
  void inFlightKeyConflictsWithoutWaiting() throws Exception {
    UUID key = UUID.randomUUID();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              jdbc.queryForObject(
                  "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                  Object.class,
                  key.toString());
              var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
              try {
                var call =
                    executor.submit(
                        () -> {
                          assertThatThrownBy(() -> service.create(key, BODY))
                              .isInstanceOfSatisfying(
                                  RequestFailure.class,
                                  error ->
                                      assertThat(error.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
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

  @TestConfiguration(proxyBeanMethods = false)
  static class Database {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
      return new PostgreSQLContainer("postgres:17-alpine");
    }
  }
}
