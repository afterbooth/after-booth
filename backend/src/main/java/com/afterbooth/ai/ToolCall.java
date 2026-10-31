package com.afterbooth.ai;

/** A function call the model asked for; {@code argumentsJson} is the raw JSON text it produced. */
public record ToolCall(String id, String name, String argumentsJson) {}
