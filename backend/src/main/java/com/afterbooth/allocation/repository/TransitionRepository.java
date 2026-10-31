package com.afterbooth.allocation.repository;

import com.afterbooth.allocation.domain.ReservationTransition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransitionRepository extends JpaRepository<ReservationTransition, Long> {
  List<ReservationTransition> findByReservationIdOrderByIdAsc(Long id);
}
