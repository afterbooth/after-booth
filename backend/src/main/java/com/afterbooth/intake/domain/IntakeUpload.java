package com.afterbooth.intake.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

public record IntakeUpload(byte[] bytes, String mediaType) {
  private static final long MAX_PIXELS = 40_000_000L;

  public IntakeUpload {
    if (bytes == null
        || bytes.length == 0
        || bytes.length > 10 * 1024 * 1024
        || mediaType == null
        || !Set.of("image/jpeg", "image/png").contains(mediaType)) invalid();
    boolean png =
        bytes.length >= 8
            && bytes[0] == (byte) 137
            && bytes[1] == 80
            && bytes[2] == 78
            && bytes[3] == 71;
    boolean jpeg =
        bytes.length >= 3
            && bytes[0] == (byte) 255
            && bytes[1] == (byte) 216
            && bytes[2] == (byte) 255;
    if (!(mediaType.equals("image/png") ? png : jpeg)) invalid();
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) invalid();
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) invalid();
        if (reader.read(0) == null) invalid();
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      invalid();
    }
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  private static void invalid() {
    throw new BusinessException(ErrorCode.INVALID_INPUT);
  }
}
