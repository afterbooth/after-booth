package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.ai.AiGateway;
import org.junit.jupiter.api.Test;

class IntakePipelineReplayTest {
  @Test
  void replaysTheExpectedInventoryFromRealPhotoRecordings() {
    IntakeDemoSupport.context("replay")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              IntakeDemoSupport.run(context.getBean(AiGateway.class));
            });
  }
}
