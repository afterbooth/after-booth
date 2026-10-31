package com.afterbooth.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AFTERBOOTH_REQUEST_REPLAY", matches = "true")
class ConditionInterpretationReplayTest {
  @Test
  void replaysReceiverRacksSedan() throws Exception {
    ConditionFixtureSupport.run("replay");
  }
}
