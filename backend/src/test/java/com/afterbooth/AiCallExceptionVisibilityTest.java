package com.afterbooth;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.ai.AiCallException;
import com.afterbooth.ai.Capability;
import org.junit.jupiter.api.Test;

/**
 * Outside com.afterbooth.ai on purpose: pipeline tests in other packages fake a failed Meta call by
 * throwing AiCallException from their own AiGateway stub.
 */
class AiCallExceptionVisibilityTest {

  @Test
  void otherPackagesCanFakeAFailedCall() {
    AiCallException failure = new AiCallException(Capability.SAM, "stubbed outage");

    assertThat(failure.capability()).isEqualTo(Capability.SAM);
    assertThat(failure.httpStatus()).isEmpty();
    assertThat(failure).hasMessage("SAM call failed: stubbed outage");
  }
}
