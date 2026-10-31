package com.afterbooth.ai;

import java.util.List;

/** Spark's answer. {@code content} is null when the model only called tools. */
public record ChatResponse(
    String content, List<ToolCall> toolCalls, String finishReason, Usage usage) {

  public ChatResponse {
    toolCalls = List.copyOf(toolCalls);
  }

  public record Usage(
      int promptTokens, int cachedPromptTokens, int completionTokens, int reasoningTokens) {}
}
