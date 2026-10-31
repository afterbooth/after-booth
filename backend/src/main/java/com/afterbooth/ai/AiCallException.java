package com.afterbooth.ai;

import java.util.OptionalInt;

/** A Meta Model API call that did not produce a usable answer. The message never holds the key. */
public class AiCallException extends RuntimeException {

  private final Capability capability;
  private final Integer httpStatus;

  AiCallException(Capability capability, Integer httpStatus, String message, Throwable cause) {
    super(capability + " call failed: " + message, cause);
    this.capability = capability;
    this.httpStatus = httpStatus;
  }

  /** Public so tests of the pipelines can fake a failed call from their own packages. */
  public AiCallException(Capability capability, String message) {
    this(capability, null, message, null);
  }

  public Capability capability() {
    return capability;
  }

  /** The HTTP status Meta answered with; empty when no response arrived or replay answered. */
  public OptionalInt httpStatus() {
    return httpStatus == null ? OptionalInt.empty() : OptionalInt.of(httpStatus);
  }
}
