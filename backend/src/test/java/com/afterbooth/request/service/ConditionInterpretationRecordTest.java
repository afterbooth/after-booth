package com.afterbooth.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "afterbooth.ai.record-smoke", matches = "true")
class ConditionInterpretationRecordTest {
  @Test
  void recordsReceiverRacksSedan() throws Exception {
    ConditionFixtureSupport.run("record");
  }
}
