package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class FixtureStoreTest {

  private final FixtureStore store = new FixtureStore(Path.of("unused"), JSON);

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
    JsonNode image = redacted("m", "AQID").path("image");

    assertThat(image.path("redactedDataUrl").stringValue()).isEqualTo("data:image/jpeg;base64");
    assertThat(image.path("bytes").asInt()).isEqualTo(3);
    // sha256 of the bytes 01 02 03
    assertThat(image.path("sha256").stringValue())
        .isEqualTo("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81");
  }

  private static JsonNode redacted(String model, String base64) {
    return FixtureStore.redact(
        JSON.readTree(
            "{\"model\":\"" + model + "\",\"image\":\"data:image/jpeg;base64," + base64 + "\"}"));
  }
}
