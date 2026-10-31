package com.afterbooth.request.domain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class WavAudio {
  private WavAudio() {}

  public static void validate(byte[] bytes, String contentType) {
    if (contentType == null
        || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("audio/wav")
        || bytes.length < 12
        || !tag(bytes, 0, "RIFF")
        || !tag(bytes, 8, "WAVE")) {
      throw RequestFailure.unsupported();
    }
    ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    if (Integer.toUnsignedLong(data.getInt(4)) + 8 != bytes.length)
      throw RequestFailure.unsupported();
    boolean format = false;
    boolean audio = false;
    int channels = 0;
    int rate = 0;
    int alignment = 0;
    int byteRate = 0;
    long samples = 0;
    int offset = 12;
    while (offset + 8 <= bytes.length) {
      long size = Integer.toUnsignedLong(data.getInt(offset + 4));
      long next = offset + 8L + size + (size % 2);
      if (next > bytes.length) throw RequestFailure.unsupported();
      if (tag(bytes, offset, "fmt ")) {
        if (format
            || size < 16
            || data.getShort(offset + 8) != 1
            || data.getShort(offset + 22) != 16) throw RequestFailure.unsupported();
        format = true;
        channels = Short.toUnsignedInt(data.getShort(offset + 10));
        rate = data.getInt(offset + 12);
        byteRate = data.getInt(offset + 16);
        alignment = Short.toUnsignedInt(data.getShort(offset + 20));
      } else if (tag(bytes, offset, "data")) {
        if (audio) throw RequestFailure.unsupported();
        audio = true;
        samples = size;
      }
      offset = (int) next;
    }
    if (!format || !audio || offset != bytes.length) throw RequestFailure.unsupported();
    if (channels != 1
        || rate != 16000
        || alignment != 2
        || byteRate != 32000
        || samples == 0
        || samples % 2 != 0
        || samples > 60L * 32000) throw RequestFailure.invalid();
  }

  private static boolean tag(byte[] bytes, int offset, String expected) {
    return new String(bytes, offset, 4, StandardCharsets.US_ASCII).equals(expected);
  }
}
