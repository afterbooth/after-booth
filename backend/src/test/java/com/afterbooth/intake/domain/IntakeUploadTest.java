package com.afterbooth.intake.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class IntakeUploadTest {
  @Test
  void acceptsRealPngAndJpeg() throws Exception {
    for (String format : new String[] {"png", "jpeg"}) {
      var output = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
      assertThat(new IntakeUpload(output.toByteArray(), "image/" + format).bytes())
          .isEqualTo(output.toByteArray());
    }
  }

  @Test
  void rejectsPngAbovePixelLimitWithoutAllocatingTheDeclaredPixels() throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
    byte[] bytes = output.toByteArray();
    ByteBuffer.wrap(bytes).putInt(16, 100_000).putInt(20, 100_000);
    var crc = new CRC32();
    crc.update(bytes, 12, 17);
    ByteBuffer.wrap(bytes).putInt(29, (int) crc.getValue());

    assertThatThrownBy(() -> new IntakeUpload(bytes, "image/png"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).errorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void rejectsJpegAbovePixelLimitWithoutAllocatingTheDeclaredPixels() throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpeg", output);
    byte[] bytes = output.toByteArray();
    int frame = -1;
    for (int i = 0; i < bytes.length - 8; i++) {
      if (bytes[i] == (byte) 0xff && bytes[i + 1] == (byte) 0xc0) {
        frame = i;
        break;
      }
    }
    assertThat(frame).isNotNegative();
    ByteBuffer.wrap(bytes).putShort(frame + 5, (short) 65_535).putShort(frame + 7, (short) 65_535);

    assertThatThrownBy(() -> new IntakeUpload(bytes, "image/jpeg"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).errorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void rejectsEmptyOversizedAndUnsupportedUploads() {
    assertThatThrownBy(() -> new IntakeUpload(new byte[0], "image/png"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> new IntakeUpload(new byte[10 * 1024 * 1024 + 1], "image/png"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> new IntakeUpload(new byte[] {1}, "image/gif"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void refusesWrongMimeAndCorruptedPhoto() throws Exception {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
    assertThatThrownBy(() -> new IntakeUpload(output.toByteArray(), "image/jpeg"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () -> new IntakeUpload(new byte[] {(byte) 137, 80, 78, 71, 0, 0, 0, 0}, "image/png"))
        .isInstanceOf(BusinessException.class);
  }
}
