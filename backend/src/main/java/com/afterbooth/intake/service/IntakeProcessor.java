package com.afterbooth.intake.service;

import com.afterbooth.intake.pipeline.EstimatedLine;
import com.afterbooth.intake.pipeline.IntakePhoto;
import com.afterbooth.intake.pipeline.IntakePipeline;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class IntakeProcessor {
  private static final Logger log = LoggerFactory.getLogger(IntakeProcessor.class);
  private final IntakePipeline pipeline;
  private final IntakeProcessingStore store;
  private final Executor executor;

  public IntakeProcessor(
      IntakePipeline pipeline,
      IntakeProcessingStore store,
      @Qualifier("intakeExecutor") Executor executor) {
    this.pipeline = pipeline;
    this.store = store;
    this.executor = executor;
  }

  public record Submitted(Long draftId, IntakePhoto photo) {}

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void submitted(Submitted event) {
    try {
      executor.execute(() -> process(event));
    } catch (RuntimeException e) {
      log.error("Cannot schedule intake {}", event.draftId(), e);
      store.fail(event.draftId(), "사진 처리를 시작하지 못했어요. 다시 업로드해 주세요.");
    }
  }

  public void process(Submitted event) {
    try {
      List<EstimatedLine> estimates = pipeline.estimate(event.photo());
      if (estimates.isEmpty()) {
        store.ready(event.draftId(), estimates);
        return;
      }
      store.segment(event.draftId());
      List<EstimatedLine> segmented;
      try {
        segmented = pipeline.segment(event.photo(), estimates);
      } catch (RuntimeException e) {
        log.warn("Intake segmentation failed for {}", event.draftId(), e);
        segmented = estimates.stream().map(EstimatedLine::samFailed).toList();
      }
      store.ready(event.draftId(), segmented);
    } catch (RuntimeException e) {
      log.warn("Intake processing failed for {}", event.draftId(), e);
      store.fail(event.draftId(), "사진 속 물품을 처리하지 못했어요. 다시 업로드해 주세요.");
    }
  }
}
