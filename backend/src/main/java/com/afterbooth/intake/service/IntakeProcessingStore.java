package com.afterbooth.intake.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.intake.domain.IntakeDraft;
import com.afterbooth.intake.domain.IntakeDraftLine;
import com.afterbooth.intake.pipeline.EstimatedLine;
import com.afterbooth.intake.repository.IntakeDraftLineRepository;
import com.afterbooth.intake.repository.IntakeDraftRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntakeProcessingStore {
  private final IntakeDraftRepository drafts;
  private final IntakeDraftLineRepository lines;

  public IntakeProcessingStore(IntakeDraftRepository drafts, IntakeDraftLineRepository lines) {
    this.drafts = drafts;
    this.lines = lines;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void segment(Long id) {
    find(id).beginSegmentation();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ready(Long id, List<EstimatedLine> estimates) {
    IntakeDraft draft = find(id);
    estimates.forEach(
        e ->
            lines.save(
                IntakeDraftLine.identify(
                    id,
                    e.category(),
                    e.count(),
                    e.boxes(),
                    e.widthCm(),
                    e.depthCm(),
                    e.heightCm(),
                    e.condition(),
                    e.confidence(),
                    e.needsReview())));
    draft.ready();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(Long id, String message) {
    find(id).fail(message);
  }

  private IntakeDraft find(Long id) {
    return drafts.findLocked(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
