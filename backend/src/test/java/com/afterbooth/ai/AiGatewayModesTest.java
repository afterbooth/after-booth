package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.fixtureFiles;
import static com.afterbooth.ai.AiTestSupport.gateway;
import static com.afterbooth.ai.AiTestSupport.read;
import static com.afterbooth.ai.AiTestSupport.sample;
import static com.afterbooth.ai.AiTestSupport.spikeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** live, record and replay against a local stub server; nothing here touches the network. */
class AiGatewayModesTest {

  private static final String KEY = "test-key-0123456789abcdef";
  private static final ChatRequest TEXT =
      new ChatRequest(
          List.of(new ChatRequest.UserMessage("진열대를 한 문장으로 설명해 주세요.", List.of())),
          null,
          List.of(),
          "minimal");

  @TempDir Path fixtures;

  @Test
  void replayAnswersARecordedSparkCallWithNoServerAndNoKey() {
    String closedServer;
    ChatResponse recorded;
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("spark-text/rack-sedan-1.json"));
      recorded = gateway(AiMode.RECORD, meta.baseUrl(), fixtures, KEY).chat(TEXT);
      closedServer = meta.baseUrl();
      assertThat(meta.received()).hasSize(1);
    }

    ChatResponse replayed = gateway(AiMode.REPLAY, closedServer, fixtures, "").chat(TEXT);

    assertThat(replayed).isEqualTo(recorded);
    assertThat(replayed.content()).contains("180cm");
    assertThat(fixtureFiles(fixtures))
        .singleElement()
        .satisfies(file -> assertThat(file.getParent().getFileName()).hasToString("spark"));
  }

  @Test
  void recordWritesASamFixtureThatReplayReadsBackWithoutAnyRequest() {
    SamRequest request = orangeBinsBoxes();
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("sam-boxes/bins-orange-stack-1.json"));
      List<SamSegment> recorded =
          gateway(AiMode.RECORD, meta.baseUrl(), fixtures, KEY).segment(request);

      List<SamSegment> replayed =
          gateway(AiMode.REPLAY, meta.baseUrl(), fixtures, "").segment(request);

      assertThat(replayed).hasSize(4).isEqualTo(recorded);
      assertThat(meta.received()).hasSize(1);
    }
  }

  @Test
  void recordWritesATranscribeFixtureThatReplayReadsBackWithoutAnyRequest() {
    byte[] wav = sample("audio-giver-van-tonight.wav");
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("transcribe/giver-van-tonight-1.json"));
      Transcription recorded =
          gateway(AiMode.RECORD, meta.baseUrl(), fixtures, KEY).transcribe(wav);

      Transcription replayed = gateway(AiMode.REPLAY, meta.baseUrl(), fixtures, "").transcribe(wav);

      assertThat(replayed).isEqualTo(recorded);
      assertThat(replayed.audioDurationMs()).isEqualTo(4800);
      assertThat(meta.received()).hasSize(1);
    }
  }

  @Test
  void recordedFixtureHoldsTheRedactedRequestAndNoKeyOrImageBytes() {
    SamRequest request = orangeBinsBoxes();
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("sam-boxes/bins-orange-stack-1.json"));
      gateway(AiMode.RECORD, meta.baseUrl(), fixtures, KEY).segment(request);
    }

    String fixture = new String(read(fixtureFiles(fixtures).getFirst()), StandardCharsets.UTF_8);
    JsonNode saved = AiTestSupport.JSON.readTree(fixture);
    JsonNode image = saved.at("/request/body/input/0/content/1/image_url");

    assertThat(saved.at("/request/method").stringValue()).isEqualTo("POST");
    assertThat(saved.at("/request/path").stringValue()).isEqualTo("/v1/responses");
    assertThat(image.path("redactedDataUrl").stringValue()).isEqualTo("data:image/jpeg;base64");
    // The same photo the spike sent, so the same SHA-256 as fixtures/spike
    assertThat(image.path("sha256").stringValue())
        .isEqualTo("ca25fb92b51fb2525421d072a96121e0e6998edd9f2823d45c3d9ed8ba3a9986");
    assertThat(saved.at("/response/status").asInt()).isEqualTo(200);
    assertThat(saved.at("/response/body/status").stringValue()).isEqualTo("completed");
    assertThat(fixture)
        .doesNotContain(KEY)
        .doesNotContain("Bearer")
        .doesNotContain(
            Base64.getEncoder().encodeToString(request.image().bytes()).substring(0, 100))
        .doesNotContain("\r\n");
  }

  @Test
  void replayFailsLoudlyWhenNoFixtureWasRecorded() {
    try (StubMetaServer meta = new StubMetaServer()) {
      AiGateway replay = gateway(AiMode.REPLAY, meta.baseUrl(), fixtures, "");

      assertThatThrownBy(() -> replay.chat(TEXT))
          .isInstanceOfSatisfying(
              MissingFixtureException.class,
              e -> {
                assertThat(e.capability()).isEqualTo(Capability.SPARK);
                assertThat(e.key()).matches("[0-9a-f]{64}");
                assertThat(e.expectedPath())
                    .isEqualTo(
                        fixtures
                            .resolve("replay/spark/" + e.key() + ".json")
                            .toAbsolutePath()
                            .normalize());
                assertThat(e.getMessage())
                    .contains("SPARK", e.key(), e.expectedPath().toString(), "AI_MODE=record");
              });
      assertThat(meta.received()).isEmpty();
    }
  }

  @Test
  void liveSendsTheKeyAndWritesNoFixture() {
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("spark-text/rack-sedan-1.json"));

      gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY).chat(TEXT);

      assertThat(meta.received())
          .singleElement()
          .satisfies(
              request -> {
                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.path()).isEqualTo("/v1/chat/completions");
                assertThat(request.authorization()).isEqualTo("Bearer " + KEY);
              });
      assertThat(fixtureFiles(fixtures)).isEmpty();
    }
  }

  @Test
  void anErrorStatusFailsWithoutLeakingTheKeyAndIsNotRecorded() {
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(401, "{\"error\":{\"message\":\"invalid key " + KEY + "\"}}");
      AiGateway record = gateway(AiMode.RECORD, meta.baseUrl(), fixtures, KEY);

      assertThatThrownBy(() -> record.chat(TEXT))
          .isInstanceOfSatisfying(
              AiCallException.class,
              e -> {
                assertThat(e.capability()).isEqualTo(Capability.SPARK);
                assertThat(e.httpStatus()).hasValue(401);
                assertThat(e.getMessage()).contains("HTTP 401").doesNotContain(KEY);
              });
      assertThat(fixtureFiles(fixtures)).isEmpty();
    }
  }

  @Test
  void anUnreachableServerIsACallFailureWithoutStatus() {
    String closedServer;
    try (StubMetaServer meta = new StubMetaServer()) {
      closedServer = meta.baseUrl();
    }
    AiGateway live = gateway(AiMode.LIVE, closedServer, fixtures, KEY);

    assertThatThrownBy(() -> live.chat(TEXT))
        .isInstanceOfSatisfying(AiCallException.class, e -> assertThat(e.httpStatus()).isEmpty());
  }

  private static SamRequest orangeBinsBoxes() {
    return new SamRequest(
        new Image(sample("bins-orange-stack.jpg"), "image/jpeg"),
        new SamRequest.Boxes(
            1280,
            960,
            List.of(
                new PixelBox(171, 237, 1172, 867),
                new PixelBox(230, 642, 1106, 959),
                new PixelBox(956, 473, 1279, 894),
                new PixelBox(959, 774, 1279, 959))));
  }
}
