package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

class FixtureStoreTest {

  private final FixtureStore store = new FixtureStore(Path.of("unused"), JSON);

  @TempDir Path fixtures;

  @Test
  void readsTheBodyOfAValidFixture() throws IOException {
    FixtureStore onDisk = new FixtureStore(fixtures, JSON);
    Path file = writeFixture(onDisk, "{\"response\":{\"status\":200,\"body\":{\"ok\":true}}}");

    assertThat(onDisk.readResponseBody(Capability.SPARK, key(file)).path("ok").asBoolean())
        .isTrue();
  }

  // Replay setup errors must not look like model failures, which callers absorb
  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"response\":{\"status\":200}}",
        "{\"response\":{\"status\":200,\"body\":\"text\"}}",
        "{\"response\":{\"status\":500,\"body\":{}}}",
        "{\"request\":{}}",
        "not json"
      })
  void aBrokenFixtureIsASetupErrorNamingTheFile(String content) throws IOException {
    FixtureStore onDisk = new FixtureStore(fixtures, JSON);
    Path file = writeFixture(onDisk, content);

    assertThatThrownBy(() -> onDisk.readResponseBody(Capability.SPARK, key(file)))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(AiCallException.class)
        .isNotInstanceOf(MissingFixtureException.class)
        .hasMessageContaining(file.toString());
  }

  private static Path writeFixture(FixtureStore store, String content) throws IOException {
    Path file = store.file(Capability.SPARK, "k".repeat(64));
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private static String key(Path file) {
    return file.getFileName().toString().replace(".json", "");
  }

  @Test
  void keyIgnoresPropertyOrder() {
    JsonNode one = JSON.readTree("{\"model\":\"m\",\"input\":{\"a\":1,\"b\":[{\"x\":1,\"y\":2}]}}");
    JsonNode other =
        JSON.readTree("{\"input\":{\"b\":[{\"y\":2,\"x\":1}],\"a\":1},\"model\":\"m\"}");

    assertThat(store.key("POST", "/v1/responses", one))
        .isEqualTo(store.key("POST", "/v1/responses", other));
  }

  @Test
  void keyChangesWithPathModelAndImage() {
    String key = store.key("POST", "/v1/responses", redacted("m", "AQID"));

    assertThat(store.key("POST", "/v1/chat/completions", redacted("m", "AQID"))).isNotEqualTo(key);
    assertThat(store.key("POST", "/v1/responses", redacted("other", "AQID"))).isNotEqualTo(key);
    assertThat(store.key("POST", "/v1/responses", redacted("m", "AQIE"))).isNotEqualTo(key);
  }

  @Test
  void redactionReplacesTheImagePayloadWithItsDigest() {
    JsonNode image = redacted("m", "AQID").path("image_url");

    assertThat(image.path("redactedDataUrl").stringValue()).isEqualTo("data:image/jpeg;base64");
    assertThat(image.path("bytes").asInt()).isEqualTo(3);
    // sha256 of the bytes 01 02 03
    assertThat(image.path("sha256").stringValue())
        .isEqualTo("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81");
  }

  @Test
  void redactionAlsoReachesSparkNestedImageUrl() {
    JsonNode redacted =
        FixtureStore.redact(
            JSON.readTree("{\"image_url\":{\"url\":\"data:image/jpeg;base64,AQID\"}}"));

    assertThat(redacted.at("/image_url/url/bytes").asInt()).isEqualTo(3);
  }

  @ParameterizedTest
  @ValueSource(strings = {"data:image/png;base64,not-valid!", "data:image/png;base64,AQID"})
  void textOutsideImageUrlIsNeverRedacted(String text) {
    JsonNode request = JSON.readTree("{\"messages\":[{\"content\":\"" + text + "\"}]}");

    assertThat(FixtureStore.redact(request)).isEqualTo(request);
  }

  @Test
  void anImageUrlThatIsNotBase64StaysAsItIs() {
    JsonNode request = JSON.readTree("{\"image_url\":\"data:image/png;base64,not-valid!\"}");

    assertThat(FixtureStore.redact(request)).isEqualTo(request);
  }

  private static JsonNode redacted(String model, String base64) {
    return FixtureStore.redact(
        JSON.readTree(
            "{\"model\":\""
                + model
                + "\",\"image_url\":\"data:image/jpeg;base64,"
                + base64
                + "\"}"));
  }
}
