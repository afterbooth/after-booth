package com.afterbooth.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Replays the real responses AiGatewayRecordSmokeTest recorded, with no key and no network. */
class AiGatewaySmokeReplayTest {

  @Test
  void replaysTheRecordedSmokeCalls() {
    assumeTrue(
        Files.isDirectory(Path.of("../fixtures/replay")),
        "fixtures/replay does not exist yet: run AiGatewayRecordSmokeTest once (fixtures/README.md)");

    AiConfigurationTest.applicationContext()
        .withPropertyValues("afterbooth.ai.mode=replay", "afterbooth.ai.api-key=")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              GatewaySmoke.callEachModelOnce(context.getBean(AiGateway.class));
            });
  }
}
