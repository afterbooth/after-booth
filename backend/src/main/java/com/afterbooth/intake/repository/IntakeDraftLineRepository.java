package com.afterbooth.intake.repository;

import com.afterbooth.intake.domain.IntakeDraftLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeDraftLineRepository extends JpaRepository<IntakeDraftLine, Long> {
  List<IntakeDraftLine> findByDraftIdOrderById(Long draftId);
}
