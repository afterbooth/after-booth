package com.afterbooth.ai;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * The one place where {@code AI_MODE} acts. live sends the request; record sends it and saves the
 * 2xx exchange as a fixture; replay answers from the fixture and never builds an HTTP client.
 */
@Component
class MetaExchange {

  private static final String METHOD = "POST";
  private static final MediaType AUDIO_WAV = MediaType.parseMediaType("audio/wav");
  private static final int ERROR_BODY_LIMIT = 500;

  private final AiProperties properties;
  private final JsonMapper json = JsonMapper.builder().build();
  private final FixtureStore fixtures;
  private final Map<Capability, RestClient> clients;

  MetaExchange(AiProperties properties) {
    this.properties = properties;
    this.fixtures = new FixtureStore(Path.of(properties.fixturesDir()), json);
    this.clients = properties.mode() == AiMode.REPLAY ? Map.of() : clients(properties);
  }

  JsonNode postJson(Capability capability, ObjectNode body) {
    return exchange(
        capability,
        FixtureStore.redact(body),
        request ->
            request.contentType(MediaType.APPLICATION_JSON).body(json.writeValueAsBytes(body)));
  }

  /** Multipart with a JSON {@code request} part and an {@code audio} WAV part. */
  JsonNode postAudio(Capability capability, ObjectNode requestPart, byte[] wav) {
    ObjectNode redacted = JsonNodeFactory.instance.objectNode();
    redacted.set("request", requestPart);
    redacted.set("audio", FixtureStore.digest(wav));
    return exchange(
        capability,
        redacted,
        request ->
            request
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(audioParts(json.writeValueAsBytes(requestPart), wav)));
  }

  private JsonNode exchange(
      Capability capability,
      JsonNode redactedBody,
      UnaryOperator<RestClient.RequestBodySpec> withBody) {
    String path = properties.paths().of(capability);
    if (properties.mode() == AiMode.REPLAY) {
      return fixtures.readResponseBody(capability, fixtures.key(METHOD, path, redactedBody));
    }
    HttpResult result = send(capability, path, withBody);
    JsonNode responseBody = parse(capability, result);
    if (properties.mode() == AiMode.RECORD) {
      fixtures.write(
          capability,
          fixtures.key(METHOD, path, redactedBody),
          METHOD,
          path,
          redactedBody,
          result.status(),
          responseBody);
    }
    return responseBody;
  }

  private HttpResult send(
      Capability capability, String path, UnaryOperator<RestClient.RequestBodySpec> withBody) {
    try {
      return withBody
          .apply(clients.get(capability).post().uri(path).accept(MediaType.APPLICATION_JSON))
          .exchange(
              (request, response) ->
                  new HttpResult(
                      response.getStatusCode().value(), response.getBody().readAllBytes()));
    } catch (RestClientException e) {
      throw new AiCallException(capability, null, scrub(String.valueOf(e.getMessage())), e);
    }
  }

  private JsonNode parse(Capability capability, HttpResult result) {
    String text = scrub(new String(result.body(), StandardCharsets.UTF_8));
    String excerpt = text.length() > ERROR_BODY_LIMIT ? text.substring(0, ERROR_BODY_LIMIT) : text;
    if (result.status() < 200 || result.status() >= 300) {
      throw new AiCallException(
          capability, result.status(), "HTTP " + result.status() + ": " + excerpt, null);
    }
    try {
      return json.readTree(result.body());
    } catch (JacksonException e) {
      throw new AiCallException(capability, result.status(), "response is not JSON: " + excerpt, e);
    }
  }

  private String scrub(String message) {
    String key = properties.apiKey();
    return key == null || key.isBlank() ? message : message.replace(key, "<redacted>");
  }

  private static MultiValueMap<String, HttpEntity<?>> audioParts(byte[] requestJson, byte[] wav) {
    MultiValueMap<String, HttpEntity<?>> parts = new LinkedMultiValueMap<>();
    parts.add("request", new HttpEntity<>(requestJson, contentType(MediaType.APPLICATION_JSON)));
    parts.add(
        "audio",
        new HttpEntity<>(
            new ByteArrayResource(wav) {
              @Override
              public String getFilename() {
                return "audio.wav";
              }
            },
            contentType(AUDIO_WAV)));
    return parts;
  }

  private static HttpHeaders contentType(MediaType mediaType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return headers;
  }

  private static Map<Capability, RestClient> clients(AiProperties properties) {
    Map<Capability, RestClient> clients = new EnumMap<>(Capability.class);
    for (Capability capability : Capability.values()) {
      // Non-streaming calls answer in one piece, so the read timeout bounds the whole call
      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setConnectTimeout(properties.timeouts().of(capability));
      factory.setReadTimeout(properties.timeouts().of(capability));
      clients.put(
          capability,
          RestClient.builder()
              .baseUrl(properties.baseUrl())
              .requestFactory(factory)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
              .build());
    }
    return clients;
  }

  private record HttpResult(int status, byte[] body) {}
}
