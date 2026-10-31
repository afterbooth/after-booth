package com.afterbooth.allocation.domain;

public class AgentHoldRejectedException extends RuntimeException {
  private final AgentHoldRejection rejection;

  public AgentHoldRejectedException(AgentHoldRejection rejection) {
    super(rejection.name());
    this.rejection = rejection;
  }

  public AgentHoldRejection rejection() {
    return rejection;
  }
}
