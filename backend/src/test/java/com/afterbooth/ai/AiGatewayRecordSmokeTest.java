package com.afterbooth.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Makes real, paid calls in record mode and writes their fixtures to fixtures/replay, one test per
 * model so a single model can be re-recorded. Off unless a human passes -PrecordSmoke with the key
 * loaded (fixtures/README.md); the credit hook asks first.
 */
@EnabledIfSystemProperty(named = "afterbooth.ai.record-smoke", matches = "true")
class AiGatewayRecordSmokeTest {

  @Test
  void recordsSpark() {
    record(GatewaySmoke::callSpark);
  }

  @Test
  void recordsSam() {
    record(GatewaySmoke::callSam);
  }

  @Test
  void recordsTranscribe() {
    record(GatewaySmoke::callTranscribe);
  }

  private static void record(Consumer<AiGateway> call) {
    AiConfigurationTest.applicationContext()
        .withPropertyValues("afterbooth.ai.mode=record")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              call.accept(context.getBean(AiGateway.class));
            });
  }
}
