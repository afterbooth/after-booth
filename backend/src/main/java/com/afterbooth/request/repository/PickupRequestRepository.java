package com.afterbooth.request.repository;

import com.afterbooth.request.domain.PickupRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from PickupRequest r where r.id=:id")
  Optional<PickupRequest> lock(@Param("id") Long id);
}
