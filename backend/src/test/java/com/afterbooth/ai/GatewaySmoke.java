package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.sample;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

/**
 * Three tiny calls, one per model, shared by the opt-in record run and the replay test, so both
 * build byte-identical requests and therefore the same fixture keys.
 */
final class GatewaySmoke {

  static final ChatRequest SPARK =
      new ChatRequest(
          List.of(new ChatRequest.UserMessage("접이식 테이블이 무엇인지 한국어 한 문장으로 답해 주세요.", List.of())),
          null,
          List.of(),
          "minimal");

  // Box from fixtures/spike/sam-boxes/table-lifetime-single-1.json; the photo has no people
  static final SamRequest SAM =
      new SamRequest(
          new Image(sample("table-lifetime-single.jpg"), "image/jpeg"),
          new SamRequest.Boxes(469, 302, List.of(new PixelBox(18, 25, 435, 284))));

  // Windows TTS, no human voice (spike/samples/SOURCES.md)
  static final byte[] WAV = sample("audio-giver-van-tonight.wav");

  private GatewaySmoke() {}

  static void callSpark(AiGateway gateway) {
    ChatResponse answer = gateway.chat(SPARK);
    assertThat(answer.content()).containsPattern("[가-힣]");
    assertThat(answer.finishReason()).isEqualTo("stop");
  }

  static void callSam(AiGateway gateway) {
    assertThat(gateway.segment(SAM))
        .singleElement()
        .satisfies(
            segment -> {
              assertThat(segment.id()).isEqualTo(1);
              assertThat(segment.score()).isPositive();
              assertThat(segment.mask().data()).isNotBlank();
            });
  }

  static void callTranscribe(AiGateway gateway) {
    Transcription transcription = gateway.transcribe(WAV);
    assertThat(transcription.transcript()).isNotBlank();
    assertThat(transcription.audioDurationMs()).isPositive();
  }
}
