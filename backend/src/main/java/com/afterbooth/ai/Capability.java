package com.afterbooth.ai;

import java.util.Locale;

/** The three Meta models the product calls. Also names the replay fixture folder. */
public enum Capability {
  SPARK,
  SAM,
  TRANSCRIBE;

  String folder() {
    return name().toLowerCase(Locale.ROOT);
  }
}
