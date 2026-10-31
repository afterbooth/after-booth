package com.afterbooth.request.service;

import com.afterbooth.ai.AiCallException;
import com.afterbooth.ai.AiGateway;
import com.afterbooth.ai.ChatRequest;
import com.afterbooth.ai.ChatResponse;
import com.afterbooth.ai.MissingFixtureException;
import com.afterbooth.request.domain.ConditionContext;
import com.afterbooth.request.domain.ConditionOutput;
import com.afterbooth.request.domain.ConditionPrompt;
import com.afterbooth.request.domain.RequestFailure;
import com.afterbooth.request.domain.WavAudio;
import com.afterbooth.request.dto.ConditionInterpretation;
import com.afterbooth.request.dto.Conditions;
import com.afterbooth.request.repository.RequestReferences;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ConditionService {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final AiGateway gateway;
  private final RequestReferences references;
  private final Clock clock;
  private final String reasoningEffort;

  public ConditionService(
      AiGateway gateway,
      RequestReferences references,
      Clock clock,
      @Value("${afterbooth.request.reasoning-effort:minimal}") String reasoningEffort) {
    this.gateway = gateway;
    this.references = references;
    this.clock = clock;
    this.reasoningEffort = reasoningEffort;
  }

  public ConditionInterpretation interpret(
      ConditionContext context, Long eventId, byte[] audio, String contentType, String text) {
    if (context == null
        || (audio == null) == (text == null)
        || (text != null && (text.isBlank() || text.length() > 500)))
      throw RequestFailure.invalid();
    LocalDate referenceDate = clock.instant().atZone(SEOUL).toLocalDate();
    if (context == ConditionContext.GIVER) {
      if (eventId == null) throw RequestFailure.invalid();
      referenceDate =
          references
              .eventDeadline(eventId)
              .orElseThrow(RequestFailure::invalid)
              .atZone(SEOUL)
              .toLocalDate();
    }
    if (audio != null) WavAudio.validate(audio, contentType);
    try {
      String transcript = audio == null ? text : gateway.transcribe(audio).transcript();
      if (transcript == null || transcript.isBlank()) throw RequestFailure.unavailable();
      List<ChatRequest.Message> messages = new ArrayList<>();
      messages.add(new ChatRequest.SystemMessage(ConditionPrompt.SYSTEM));
      messages.add(
          new ChatRequest.UserMessage(
              JSON.writeValueAsString(
                  new Input(
                      context,
                      clock.instant().atZone(SEOUL).toOffsetDateTime().toString(),
                      referenceDate.toString(),
                      transcript)),
              List.of()));
      for (int attempt = 0; attempt < 2; attempt++) {
        ChatResponse response =
            gateway.chat(
                new ChatRequest(
                    messages,
                    new ChatRequest.ResponseSchema("pickup_conditions", ConditionPrompt.SCHEMA),
                    List.of(),
                    reasoningEffort));
        try {
          if (!response.toolCalls().isEmpty() || !"stop".equals(response.finishReason()))
            throw new IllegalArgumentException("Incomplete conditions");
          Conditions conditions = ConditionOutput.parse(response.content());
          List<String> missing = new ArrayList<>();
          if (context == ConditionContext.RECEIVER) {
            if (conditions.category() == null) missing.add("category");
            if (conditions.vehicleType() == null) missing.add("vehicleType");
          }
          return new ConditionInterpretation(context, transcript, conditions, List.copyOf(missing));
        } catch (IllegalArgumentException | JacksonException | DateTimeException e) {
          if (attempt == 1) throw RequestFailure.unavailable();
          messages.add(
              new ChatRequest.AssistantMessage(
                  response.content() == null ? "" : response.content(), List.of()));
          messages.add(
              new ChatRequest.UserMessage(
                  "The previous output failed server validation. Return only a complete JSON object matching the schema, with valid enums, quantity >= 1 and an ISO-8601 offset deadline. Omit unknown fields and never emit null.",
                  List.of()));
        }
      }
      throw RequestFailure.unavailable();
    } catch (AiCallException | MissingFixtureException e) {
      throw RequestFailure.unavailable();
    }
  }

  private record Input(
      ConditionContext context, String currentTime, String referenceDate, String transcript) {}
}
