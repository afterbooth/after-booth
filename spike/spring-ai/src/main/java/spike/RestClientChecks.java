package spike;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Fallback path: the same three checks as raw OpenAI-compatible Chat Completions over RestClient. */
@Component
class RestClientChecks {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
      new ParameterizedTypeReference<>() {};
  private static final Pattern ITEM_ID = Pattern.compile("\"itemId\"\\s*:\\s*\"?(\\d+)");
  private static final Pattern VEHICLE = Pattern.compile("\"vehicleType\"\\s*:\\s*\"(\\w+)\"");

  private final RestClient http;
  private final String model;
  private final CallLog log;

  RestClientChecks(
      @Value("${spring.ai.openai.base-url}") String baseUrl,
      @Value("${spring.ai.openai.api-key}") String apiKey,
      @Value("${spring.ai.openai.chat.options.model}") String model,
      CallLog log) {
    this.http =
        RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    this.model = model;
    this.log = log;
  }

  void run(String image) {
    text();
    tools();
    image(image);
  }

  private void text() {
    long started = System.nanoTime();
    try {
      Map<String, Object> response =
          post(
              List.of(
                  Map.of(
                      "role",
                      "user",
                      "content",
                      "높이 180cm 진열대가 왜 일반 승용차에 실리지 않는지 한국어 한 문장으로 설명해 주세요.")),
              null);
      ok("rest-text", "rack-sedan", started, usage(response, new int[2]), 1, content(response));
    } catch (RuntimeException e) {
      failed("rest-text", "rack-sedan", started, 1, e);
    }
  }

  private void tools() {
    long started = System.nanoTime();
    VehicleTools tools = new VehicleTools();
    List<Object> toolDefs =
        List.of(
            Map.of(
                "type",
                "function",
                "function",
                Map.of(
                    "name",
                    "checkVehicleFit",
                    "description",
                    "Check on the server whether one inventory item fits the cargo space of a vehicle type.",
                    "parameters",
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                            "itemId",
                            Map.of("type", "integer"),
                            "vehicleType",
                            Map.of("type", "string", "enum", List.of("SEDAN", "SUV", "VAN", "TRUCK_1T"))),
                        "required",
                        List.of("itemId", "vehicleType")))));
    List<Object> messages = new ArrayList<>();
    messages.add(
        Map.of(
            "role",
            "user",
            "content",
            "물품 101번 진열대를 승용차(SEDAN)로 가져갈 수 있는지 checkVehicleFit 도구로 확인하고 한국어 한 문장으로 답해 주세요."));
    int[] tokens = new int[2];
    int calls = 0;
    try {
      for (int turn = 1; turn <= 4; turn++) {
        Map<String, Object> response = post(messages, toolDefs);
        calls++;
        usage(response, tokens);
        Map<String, Object> message = message(response);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls == null || toolCalls.isEmpty()) {
          ok("rest-tools", "check-vehicle-fit", started, tokens, calls, content(response));
          return;
        }
        Map<String, Object> assistant = new HashMap<>(message);
        assistant.putIfAbsent("content", "");
        messages.add(assistant);
        for (Map<String, Object> call : toolCalls) {
          @SuppressWarnings("unchecked")
          String arguments = (String) ((Map<String, Object>) call.get("function")).get("arguments");
          VehicleTools.FitResult fit =
              tools.checkVehicleFit(
                  Long.parseLong(find(ITEM_ID, arguments)), find(VEHICLE, arguments));
          messages.add(
              Map.of(
                  "role",
                  "tool",
                  "tool_call_id",
                  call.get("id"),
                  "content",
                  String.format(
                      "{\"itemId\":%d,\"vehicleType\":\"%s\",\"fits\":%b,\"code\":%s,\"message\":%s}",
                      fit.itemId(),
                      fit.vehicleType(),
                      fit.fits(),
                      fit.code() == null ? "null" : "\"" + fit.code() + "\"",
                      fit.message() == null ? "null" : "\"" + fit.message() + "\"")));
        }
      }
      failed("rest-tools", "check-vehicle-fit", started, calls, new IllegalStateException("no final answer after 4 turns"));
    } catch (RuntimeException e) {
      failed("rest-tools", "check-vehicle-fit", started, calls, e);
    }
  }

  private void image(String image) {
    long started = System.nanoTime();
    try {
      String dataUrl =
          "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(image)));
      Map<String, Object> response =
          post(
              List.of(
                  Map.of(
                      "role",
                      "user",
                      "content",
                      List.of(
                          Map.of("type", "text", "text", "이 사진 속 물건이 무엇인지, 대략 몇 cm 크기인지 한국어 한 문장으로 답해 주세요."),
                          Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))),
              null);
      ok("rest-image", "rack-gondola-single", started, usage(response, new int[2]), 1, content(response));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      failed("rest-image", "rack-gondola-single", started, 1, e);
    }
  }

  private Map<String, Object> post(List<Object> messages, List<Object> tools) {
    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    if (tools != null) {
      body.put("tools", tools);
    }
    return http.post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JSON_MAP);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> message(Map<String, Object> response) {
    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
    return (Map<String, Object>) choices.getFirst().get("message");
  }

  private static String content(Map<String, Object> response) {
    return String.valueOf(message(response).get("content"));
  }

  /** Adds this response's prompt and completion tokens to {@code tokens} and returns it. */
  private static int[] usage(Map<String, Object> response, int[] tokens) {
    if (response.get("usage") instanceof Map<?, ?> usage) {
      tokens[0] += ((Number) usage.get("prompt_tokens")).intValue();
      tokens[1] += ((Number) usage.get("completion_tokens")).intValue();
    }
    return tokens;
  }

  private static String find(Pattern pattern, String text) {
    Matcher m = pattern.matcher(text);
    if (!m.find()) {
      throw new IllegalArgumentException("tool arguments missing " + pattern + ": " + text);
    }
    return m.group(1);
  }

  private void ok(String capability, String inputId, long started, int[] tokens, int calls, String answer) {
    log.record(
        new CallLog.Result(
            capability, inputId, true, elapsed(started), tokens[0], tokens[1], calls, answer, null));
  }

  private void failed(String capability, String inputId, long started, int calls, RuntimeException e) {
    String error = e.getClass().getName() + ": " + e.getMessage();
    if (e instanceof RestClientResponseException http) {
      error = "HTTP " + http.getStatusCode().value() + " " + http.getResponseBodyAsString();
    }
    log.record(
        new CallLog.Result(capability, inputId, false, elapsed(started), null, null, calls, null, error));
  }

  private static long elapsed(long started) {
    return (System.nanoTime() - started) / 1_000_000;
  }
}
