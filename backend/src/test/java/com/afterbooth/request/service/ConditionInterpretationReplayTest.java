package com.afterbooth.request.service;

import org.junit.jupiter.api.Test;

class ConditionInterpretationReplayTest {
  @Test
  void replaysReceiverRacksSedan() throws Exception {
    ConditionFixtureSupport.run("replay");
  }
}
