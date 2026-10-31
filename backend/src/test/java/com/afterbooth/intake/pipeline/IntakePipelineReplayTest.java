package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.afterbooth.ai.AiGateway;
import com.afterbooth.ai.MissingFixtureException;
import org.junit.jupiter.api.Test;

class IntakePipelineReplayTest {
  @Test
  void replaysTheExpectedInventoryFromRealPhotoRecordings() {
    IntakeDemoSupport.context("replay")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              try {
                IntakeDemoSupport.run(context.getBean(AiGateway.class));
              } catch (MissingFixtureException missing) {
                assumeTrue(
                    false,
                    "Real intake recording is absent: "
                        + missing.expectedPath()
                        + ". A human must run IntakePipelineRecordSmokeTest with -PrecordSmoke first.");
              }
            });
  }
}
