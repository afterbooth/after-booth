package com.afterbooth.ai;

import java.util.List;

/**
 * One Muse Spark chat completion. {@code responseSchema} and {@code reasoningEffort} may be null.
 * The gateway never sends {@code stop}, {@code n}, {@code logprobs}, penalties or a {@code
 * tool_choice} other than the default {@code auto}: the Meta API rejects them (spike/REPORT.md §2).
 */
public record ChatRequest(
    List<Message> messages,
    ResponseSchema responseSchema,
    List<Tool> tools,
    String reasoningEffort) {

  public ChatRequest {
    messages = List.copyOf(messages);
    tools = List.copyOf(tools);
    if (messages.isEmpty()) {
      throw new IllegalArgumentException("A Spark chat request needs at least one message");
    }
    if ("none".equals(reasoningEffort)) {
      throw new IllegalArgumentException(
          "The Meta API rejects reasoning_effort \"none\" with HTTP 400; pass null for the model"
              + " default");
    }
  }

  public sealed interface Message
      permits SystemMessage, UserMessage, AssistantMessage, ToolMessage {}

  public record SystemMessage(String text) implements Message {}

  public record UserMessage(String text, List<Image> images) implements Message {
    public UserMessage {
      images = List.copyOf(images);
    }
  }

  /** A previous assistant turn, replayed so the caller can run its own tool loop. */
  public record AssistantMessage(String text, List<ToolCall> toolCalls) implements Message {
    public AssistantMessage {
      toolCalls = List.copyOf(toolCalls);
    }
  }

  /** The server's result for one tool call, as JSON text. */
  public record ToolMessage(String toolCallId, String content) implements Message {}

  /** Structured output: {@code schemaJson} is a JSON Schema object. */
  public record ResponseSchema(String name, String schemaJson) {}

  /** A function the model may call; {@code parametersJson} is a JSON Schema object. */
  public record Tool(String name, String description, String parametersJson) {}
}
