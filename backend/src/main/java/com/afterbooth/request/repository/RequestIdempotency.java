package com.afterbooth.request.repository;

import com.afterbooth.request.domain.RequestFailure;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RequestIdempotency {
  private final JdbcTemplate jdbc;

  public RequestIdempotency(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String begin(UUID key, String fingerprint, Instant now) {
    // Transaction-scoped, nonblocking lock makes concurrent same-key requests fail immediately.
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select pg_try_advisory_xact_lock(hashtextextended(?, 0))",
            Boolean.class,
            key.toString()))) throw RequestFailure.conflict();
    List<Stored> rows =
        jdbc.query(
            "select fingerprint, state, response_body::text from idempotency_record where idempotency_key = ?",
            (row, index) -> new Stored(row.getString(1), row.getString(2), row.getString(3)),
            key);
    if (!rows.isEmpty()) {
      Stored stored = rows.getFirst();
      if (!stored.fingerprint().equals(fingerprint) || !stored.state().equals("DONE"))
        throw RequestFailure.conflict();
      return stored.body();
    }
    jdbc.update(
        "insert into idempotency_record(idempotency_key, fingerprint, state, created_at) values (?, ?, 'IN_PROGRESS', ?)",
        key,
        fingerprint,
        Timestamp.from(now));
    return null;
  }

  public void complete(UUID key, String body) {
    jdbc.update(
        "update idempotency_record set state = 'DONE', response_status = 201, response_body = cast(? as jsonb) where idempotency_key = ?",
        body,
        key);
  }

  private record Stored(String fingerprint, String state, String body) {}
}
