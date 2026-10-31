package com.afterbooth.global.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {
  private final JdbcTemplate jdbc;

  public IdempotencyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean claim(UUID key, String fingerprint, Instant now) {
    return jdbc.update(
            "INSERT INTO idempotency_record(idempotency_key,fingerprint,state,created_at) VALUES (?,?,'IN_PROGRESS',?) ON CONFLICT DO NOTHING",
            key,
            fingerprint,
            java.sql.Timestamp.from(now))
        == 1;
  }

  public Optional<Saved> find(UUID key) {
    return jdbc
        .query(
            "SELECT fingerprint,state,response_body::text,response_status FROM idempotency_record WHERE idempotency_key=?",
            (rs, n) ->
                new Saved(
                    rs.getString(1), rs.getString(2), rs.getString(3), (Integer) rs.getObject(4)),
            key)
        .stream()
        .findFirst();
  }

  public void finish(UUID key, int status, String body) {
    jdbc.update(
        "UPDATE idempotency_record SET state='DONE',response_status=?,response_body=CAST(? AS jsonb) WHERE idempotency_key=?",
        status,
        body,
        key);
  }

  public record Saved(String fingerprint, String state, String body, Integer status) {}
}
