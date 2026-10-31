package com.afterbooth.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Makes three real, paid calls in record mode and writes their fixtures to fixtures/replay. Off
 * unless a human passes -PrecordSmoke with the key loaded (fixtures/README.md); the credit hook
 * asks first.
 */
@EnabledIfSystemProperty(named = "afterbooth.ai.record-smoke", matches = "true")
class AiGatewayRecordSmokeTest {

  @Test
  void recordsOneCallPerModel() {
    AiConfigurationTest.applicationContext()
        .withPropertyValues("afterbooth.ai.mode=record")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              GatewaySmoke.callEachModelOnce(context.getBean(AiGateway.class));
            });
  }
}
