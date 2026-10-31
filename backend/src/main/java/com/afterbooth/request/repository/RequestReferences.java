package com.afterbooth.request.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RequestReferences {
  private final JdbcTemplate jdbc;

  public RequestReferences(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Instant> eventDeadline(Long id) {
    return jdbc
        .query(
            "select teardown_deadline from event where id = ?",
            (row, index) -> row.getTimestamp(1).toInstant(),
            id)
        .stream()
        .findFirst();
  }

  public boolean organizationExists(Long id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from organization where id = ?)", Boolean.class, id));
  }
}
