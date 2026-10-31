package com.afterbooth.handover.repository;

import com.afterbooth.handover.domain.HandoverLog;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface HandoverLogRepository extends JpaRepository<HandoverLog, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<HandoverLog> findFirstByReservationIdOrderByIdDesc(Long reservationId);
}
