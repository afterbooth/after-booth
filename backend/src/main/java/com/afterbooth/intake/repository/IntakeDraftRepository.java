package com.afterbooth.intake.repository;

import com.afterbooth.intake.domain.IntakeDraft;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntakeDraftRepository extends JpaRepository<IntakeDraft, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from IntakeDraft d where d.id = :id")
  Optional<IntakeDraft> findLocked(@Param("id") Long id);
}
