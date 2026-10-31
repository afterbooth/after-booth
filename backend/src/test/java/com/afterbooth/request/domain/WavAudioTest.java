package com.afterbooth.request.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WavAudioTest {
  public static byte[] wav(int channels, int rate, int bits, int codec, int dataLength) {
    ByteBuffer out = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN);
    out.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataLength);
    out.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
    out.putShort((short) codec).putShort((short) channels).putInt(rate);
    out.putInt(rate * channels * bits / 8)
        .putShort((short) (channels * bits / 8))
        .putShort((short) bits);
    out.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataLength);
    return out.array();
  }

  @Test
  void acceptsExactlySixtySeconds() {
    assertThatCode(() -> WavAudio.validate(wav(1, 16000, 16, 1, 1920000), "audio/wav"))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({"2,16000,32000", "1,8000,16000", "1,16000,1920002", "1,16000,0"})
  void rejectsWrongChannelsRateDurationOrEmptyAudio(int channels, int rate, int length) {
    assertThatThrownBy(() -> WavAudio.validate(wav(channels, rate, 16, 1, length), "audio/wav"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error ->
                org.assertj.core.api.Assertions.assertThat(error.errorCode().name())
                    .isEqualTo("INVALID_INPUT"));
  }

  @ParameterizedTest
  @CsvSource({"1,8", "3,16", "6,16"})
  void rejectsNonPcmOrWrongBitDepth(int codec, int bits) {
    assertThatThrownBy(() -> WavAudio.validate(wav(1, 16000, bits, codec, 32000), "audio/wav"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error ->
                org.assertj.core.api.Assertions.assertThat(error.errorCode().status().value())
                    .isEqualTo(415));
  }

  @Test
  void rejectsWrongMimeAndContainer() {
    assertThatThrownBy(() -> WavAudio.validate(wav(1, 16000, 16, 1, 32000), "audio/webm"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> WavAudio.validate(new byte[44], "audio/wav"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rejectsTruncatedChunkWithoutReadingOutOfBounds() {
    byte[] bytes = wav(1, 16000, 16, 1, 2);
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(40, Integer.MAX_VALUE);
    assertThatThrownBy(() -> WavAudio.validate(bytes, "audio/wav"))
        .isInstanceOf(BusinessException.class);
  }
}
