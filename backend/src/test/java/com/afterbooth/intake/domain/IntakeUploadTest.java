package com.afterbooth.intake.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
