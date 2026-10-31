package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.ai.AiGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "afterbooth.ai.record-smoke", matches = "true")
class IntakePipelineRecordSmokeTest {
  @Test
  void recordsTheRealPhotoPipeline() {
    IntakeDemoSupport.context("record")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              IntakeDemoSupport.run(context.getBean(AiGateway.class));
            });
  }
}
