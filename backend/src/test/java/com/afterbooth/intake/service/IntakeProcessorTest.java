package com.afterbooth.intake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.intake.domain.IntakeDraft;
import com.afterbooth.intake.domain.IntakeStage;
import com.afterbooth.intake.domain.IntakeStatus;
import com.afterbooth.intake.pipeline.EstimatedLine;
import com.afterbooth.intake.pipeline.IntakePhoto;
import com.afterbooth.intake.pipeline.IntakePipeline;
import com.afterbooth.inventory.domain.Condition;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntakeProcessorTest {
  private final IntakePipeline pipeline = mock(IntakePipeline.class);
  private final IntakeProcessingStore store = mock(IntakeProcessingStore.class);
  private final IntakePhoto photo = new IntakePhoto(new byte[] {1}, "image/png");
  private final IntakeProcessor.Submitted event = new IntakeProcessor.Submitted(1L, photo);
  private final EstimatedLine line =
      new EstimatedLine(
          DraftCategory.DISPLAY_RACK,
          1,
          List.of(new BoundingBox(0, 0, 0.2, 0.2)),
          90,
          40,
          180,
          Condition.GOOD,
          new BigDecimal("0.95"),
          false);
  private final IntakeProcessor processor = new IntakeProcessor(pipeline, store, Runnable::run);

  @Test
  void commitsSegmentStageBeforeSamAndThenReady() {
    when(pipeline.estimate(photo)).thenReturn(List.of(line));
    when(pipeline.segment(photo, List.of(line))).thenReturn(List.of(line));
    processor.process(event);
    var order = inOrder(pipeline, store);
    order.verify(pipeline).estimate(photo);
    order.verify(store).segment(1L);
    order.verify(pipeline).segment(photo, List.of(line));
    order.verify(store).ready(1L, List.of(line));
  }

  @Test
  void estimationFailureFailsWithKoreanMessageAndSkipsSam() {
    when(pipeline.estimate(photo)).thenThrow(new IllegalStateException());
    processor.process(event);
    var message = ArgumentCaptor.forClass(String.class);
    verify(store).fail(eq(1L), message.capture());
    assertThat(message.getValue()).matches(".*[\uac00-\ud7a3].*");
    verify(pipeline, never()).segment(any(), any());
  }

  @Test
  void samFailureKeepsSparkBoxesAndMarksEveryLineForReview() {
    when(pipeline.estimate(photo)).thenReturn(List.of(line));
    when(pipeline.segment(photo, List.of(line))).thenThrow(new IllegalStateException());
    processor.process(event);
    verify(store)
        .ready(
            1L,
            List.of(
                new EstimatedLine(
                    line.category(),
                    line.count(),
                    line.boxes(),
                    90,
                    40,
                    180,
                    line.condition(),
                    line.confidence(),
                    true)));
    verify(store, never()).fail(any(), any());
  }

  @Test
  void zeroItemsSkipsSamAndBecomesReady() {
    when(pipeline.estimate(photo)).thenReturn(List.of());
    processor.process(event);
    verify(store).ready(1L, List.of());
    verify(store, never()).segment(any());
    verify(pipeline, never()).segment(any(), any());
  }

  @Test
  void rejectionFailsDraftRatherThanLeavingItProcessing() {
    new IntakeProcessor(
            pipeline,
            store,
            task -> {
              throw new RejectedExecutionException();
            })
        .submitted(event);
    verify(store).fail(eq(1L), anyString());
    verifyNoInteractions(pipeline);
  }

  @Test
  void twoInvalidSparkAnswersFailTheActualDraftAtEstimate() {
    var gateway = mock(com.afterbooth.ai.AiGateway.class);
    when(gateway.chat(any()))
        .thenReturn(new com.afterbooth.ai.ChatResponse("{}", List.of(), "stop", null));
    var realPipeline =
        new IntakePipeline(
            gateway,
            tools.jackson.databind.json.JsonMapper.builder().build(),
            "minimal",
            new BigDecimal("0.8"),
            0.5);
    var draft = IntakeDraft.start(1L, java.time.Instant.EPOCH);
    doAnswer(
            invocation -> {
              draft.fail(invocation.getArgument(1));
              return null;
            })
        .when(store)
        .fail(eq(1L), anyString());
    new IntakeProcessor(realPipeline, store, Runnable::run).process(event);
    assertThat(draft.status()).isEqualTo(IntakeStatus.FAILED);
    assertThat(draft.stage()).isEqualTo(IntakeStage.ESTIMATE);
    assertThat(draft.failureMessage()).matches(".*[\uac00-\ud7a3].*");
    verify(gateway, times(2)).chat(any());
    verify(gateway, never()).segment(any());
    verify(store, never()).ready(any(), any());
  }
}
