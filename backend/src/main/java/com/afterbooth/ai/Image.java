package com.afterbooth.ai;

import java.util.Base64;

/** An image sent inline as a data URL, e.g. {@code image/jpeg}. */
public record Image(byte[] bytes, String mimeType) {

  String dataUrl() {
    return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
  }
}
