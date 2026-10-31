package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.JSON;
import static com.afterbooth.ai.AiTestSupport.gateway;
import static com.afterbooth.ai.AiTestSupport.sample;
import static com.afterbooth.ai.AiTestSupport.spikeExchange;
import static com.afterbooth.ai.AiTestSupport.spikeResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

/** Request shapes from spike/REPORT.md §2 and parsing of real spike responses, in live mode. */
class MetaAiGatewayTest {

  private static final String KEY = "test-key";

  @TempDir Path fixtures;

  @Test
  void sparkSendsMessagesImagesSchemaToolsAndEffortAndReturnsToolCalls() {
    ChatRequest request =
        new ChatRequest(
            List.of(
                new ChatRequest.SystemMessage("You are the AfterBooth allocation agent."),
                new ChatRequest.UserMessage(
                    "진열대 2개, 승용차", List.of(new Image(new byte[] {1, 2, 3}, "image/jpeg"))),
                new ChatRequest.AssistantMessage(
                    "확인 중입니다.",
                    List.of(
                        new ToolCall(
                            "call_1", "searchCandidates", "{\"category\":\"DISPLAY_RACK\"}"))),
                new ChatRequest.ToolMessage("call_1", "{\"items\":[]}")),
            new ChatRequest.ResponseSchema(
                "Answer", "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}}}"),
            List.of(
                new ChatRequest.Tool(
                    "checkVehicleFit",
                    "Check whether one item fits the vehicle.",
                    "{\"type\":\"object\",\"required\":[\"itemId\"],"
                        + "\"properties\":{\"itemId\":{\"type\":\"integer\"}}}")),
            "minimal");

    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("spark-tools/run1-turn2-1.json"));

      ChatResponse response = gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY).chat(request);

      JsonNode sent = JSON.readTree(meta.received().getFirst().body());
      assertThat(meta.received().getFirst().contentType()).startsWith("application/json");
      assertThat(sent.path("model").stringValue()).isEqualTo("muse-spark-1.3");
      assertThat(sent.at("/messages/0/role").stringValue()).isEqualTo("system");
      assertThat(sent.at("/messages/1/content/0/text").stringValue()).isEqualTo("진열대 2개, 승용차");
      assertThat(sent.at("/messages/1/content/1/image_url/url").stringValue())
          .isEqualTo("data:image/jpeg;base64,AQID");
      assertThat(sent.at("/messages/2/tool_calls/0/function/arguments").stringValue())
          .isEqualTo("{\"category\":\"DISPLAY_RACK\"}");
      assertThat(sent.at("/messages/3/role").stringValue()).isEqualTo("tool");
      assertThat(sent.at("/messages/3/tool_call_id").stringValue()).isEqualTo("call_1");
      assertThat(sent.at("/response_format/type").stringValue()).isEqualTo("json_schema");
      assertThat(sent.at("/response_format/json_schema/schema/properties/text/type").stringValue())
          .isEqualTo("string");
      assertThat(sent.at("/tools/0/type").stringValue()).isEqualTo("function");
      assertThat(sent.at("/tools/0/function/parameters/required/0").stringValue())
          .isEqualTo("itemId");
      assertThat(sent.path("reasoning_effort").stringValue()).isEqualTo("minimal");
      assertThat(keys(sent))
          .containsExactlyInAnyOrder(
              "model", "messages", "response_format", "tools", "reasoning_effort");

      assertThat(response.finishReason()).isEqualTo("tool_calls");
      assertThat(response.toolCalls())
          .extracting(ToolCall::name, ToolCall::argumentsJson)
          .containsExactly(
              tuple("checkVehicleFit", "{\"itemId\":101,\"vehicleType\":\"SEDAN\"}"),
              tuple("checkVehicleFit", "{\"itemId\":102,\"vehicleType\":\"SEDAN\"}"));
      assertThat(response.usage()).isEqualTo(new ChatResponse.Usage(1273, 0, 197, 39));
    }
  }

  @Test
  void sparkSendsTextOnlyUserContentAsAPlainStringAndOmitsUnsetOptions() {
    ChatRequest request =
        new ChatRequest(
            List.of(new ChatRequest.UserMessage("안녕하세요", List.of())), null, List.of(), null);
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("spark-text/rack-sedan-1.json"));

      ChatResponse response = gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY).chat(request);

      JsonNode sent = JSON.readTree(meta.received().getFirst().body());
      assertThat(sent.at("/messages/0/content").stringValue()).isEqualTo("안녕하세요");
      assertThat(keys(sent)).containsExactlyInAnyOrder("model", "messages");
      assertThat(response.content()).startsWith("일반 승용차는");
      assertThat(response.toolCalls()).isEmpty();
      assertThat(response.usage().reasoningTokens()).isEqualTo(692);
    }
  }

  @Test
  void sparkRejectsReasoningEffortNoneBeforeAnyCall() {
    assertThatThrownBy(
            () ->
                new ChatRequest(
                    List.of(new ChatRequest.UserMessage("hi", List.of())), null, List.of(), "none"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reasoning_effort");
  }

  @Test
  void sparkResponseWithoutChoicesIsACallFailure() {
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, "{}");
      AiGateway live = gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY);

      assertThatThrownBy(
              () ->
                  live.chat(
                      new ChatRequest(
                          List.of(new ChatRequest.UserMessage("hi", List.of())),
                          null,
                          List.of(),
                          null)))
          .isInstanceOf(AiCallException.class)
          .hasMessageContaining("no choices");
    }
  }

  @Test
  void samSendsBoxPromptsInSourcePixelsExactlyAsTheSpikeDid() {
    JsonNode spike = spikeExchange("sam-boxes/bins-orange-stack-1.json");
    SamRequest request =
        new SamRequest(
            new Image(sample("bins-orange-stack.jpg"), "image/jpeg"),
            new SamRequest.Boxes(
                1280,
                960,
                List.of(
                    new PixelBox(171, 237, 1172, 867),
                    new PixelBox(230, 642, 1106, 959),
                    new PixelBox(956, 473, 1279, 894),
                    new PixelBox(959, 774, 1279, 959))));
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spike.path("response").path("body").toString());

      List<SamSegment> segments =
          gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY).segment(request);

      JsonNode sent = JSON.readTree(meta.received().getFirst().body());
      assertThat(meta.received().getFirst().path()).isEqualTo("/v1/responses");
      assertThat(sent.path("model").stringValue()).isEqualTo("sam-3.1");
      assertThat(sent.at("/input/0/content/0/text").stringValue())
          .isEqualTo(spike.at("/request/body/input/0/content/0/text").stringValue());
      assertThat(sent.at("/input/0/content/1/image_url").stringValue())
          .startsWith("data:image/jpeg;base64,");
      assertThat(sent.path("metadata"))
          .isEqualTo(
              JSON.readTree("{\"include_confidence\":\"true\",\"mask_encoding\":\"one_bit\"}"));

      assertThat(segments).extracting(SamSegment::id).containsExactly(1, 2, 3, 4);
      SamSegment first = segments.getFirst();
      assertThat(first.box()).isEqualTo(new PixelBox(189, 228, 1167, 890));
      assertThat(first.score()).isEqualTo(0.984375);
      assertThat(first.mask().x()).isZero();
      assertThat(first.mask().data()).startsWith("663,979,");
    }
  }

  @Test
  void samConceptPromptWithNothingFoundGivesNoSegments() {
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("sam/table-picnic-folding--FOLDING_TABLE-1.json"));

      List<SamSegment> segments =
          gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY)
              .segment(
                  new SamRequest(
                      new Image(new byte[] {1}, "image/jpeg"),
                      new SamRequest.Concept("folding table")));

      JsonNode sent = JSON.readTree(meta.received().getFirst().body());
      assertThat(sent.at("/input/0/content/0/text").stringValue()).isEqualTo("folding table");
      assertThat(segments).isEmpty();
    }
  }

  @Test
  void samRejectsMoreThanSixteenBoxesBeforeAnyCall() {
    List<PixelBox> seventeen = Collections.nCopies(17, new PixelBox(0, 0, 10, 10));

    assertThatThrownBy(() -> new SamRequest.Boxes(100, 100, seventeen))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("16");
  }

  @Test
  void samResponseThatIsNotCompletedIsACallFailure() {
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, "{\"status\":\"incomplete\",\"output\":[]}");
      AiGateway live = gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY);

      assertThatThrownBy(
              () ->
                  live.segment(
                      new SamRequest(
                          new Image(new byte[] {1}, "image/jpeg"),
                          new SamRequest.Concept("table"))))
          .isInstanceOf(AiCallException.class)
          .hasMessageContaining("incomplete");
    }
  }

  @Test
  void transcribeSendsAWavMultipartWithTheConfiguredLanguageBias() {
    byte[] wav = sample("audio-giver-van-tonight.wav");
    try (StubMetaServer meta = new StubMetaServer()) {
      meta.respond(200, spikeResponse("transcribe/giver-van-tonight-1.json"));

      Transcription transcription =
          gateway(AiMode.LIVE, meta.baseUrl(), fixtures, KEY).transcribe(wav);

      StubMetaServer.Received sent = meta.received().getFirst();
      String body = new String(sent.body(), StandardCharsets.ISO_8859_1);
      assertThat(sent.path()).isEqualTo("/v1/asr/transcribe");
      assertThat(sent.contentType()).startsWith("multipart/form-data");
      assertThat(body)
          .contains("name=\"request\"")
          .contains(
              "{\"model\":\"muse-voice-transcribe-1.0\",\"audioEncoding\":\"WAV\","
                  + "\"mode\":\"PUSH_TO_TALK\",\"languageBias\":[\"Korean\"]}")
          .contains("name=\"audio\"; filename=\"audio.wav\"")
          .contains("Content-Type: audio/wav")
          .contains(new String(wav, StandardCharsets.ISO_8859_1));
      assertThat(transcription.transcript()).isEqualTo("오늘 저녁 여덟 시까지 철수해야 하고 승합차가 필요해요");
      assertThat(transcription.audioDurationMs()).isEqualTo(4800);
    }
  }

  private static List<String> keys(JsonNode node) {
    return node.properties().stream().map(Map.Entry::getKey).toList();
  }
}
