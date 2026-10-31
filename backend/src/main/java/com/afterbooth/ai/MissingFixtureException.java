package com.afterbooth.ai;

import java.nio.file.Path;

/**
 * Replay found no recorded response for a request. Deliberately not an {@link AiCallException}:
 * this is a setup error to fix, never a model failure to show the user.
 */
public class MissingFixtureException extends IllegalStateException {

  private final Capability capability;
  private final String key;
  private final Path expectedPath;

  MissingFixtureException(Capability capability, String key, Path expectedPath) {
    super(
        "No replay fixture for "
            + capability
            + " request "
            + key
            + ": expected "
            + expectedPath
            + ". Replay never calls the Meta API and never invents a response. Record it with"
            + " AI_MODE=record (a human runs it with the key, see fixtures/README.md), or check"
            + " whether the prompt, model ID, schema or input file changed, since any change"
            + " gives a new hash.");
    this.capability = capability;
    this.key = key;
    this.expectedPath = expectedPath;
  }

  public Capability capability() {
    return capability;
  }

  public String key() {
    return key;
  }

  public Path expectedPath() {
    return expectedPath;
  }
}
