package spike;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Prints each check and appends it to spike/out/calls.jsonl in the format summarize.mjs reads. */
@Component
class CallLog {

  // Muse Spark Standard mode, https://dev.meta.ai/docs/pricing-rate-limits
  private static final double INPUT_PER_TOKEN = 1.25 / 1_000_000;
  private static final double OUTPUT_PER_TOKEN = 4.25 / 1_000_000;

  private final Path file;
  private final String runId = "java-" + Instant.now().toString().replace(':', '-');
  private final List<String> lines = new ArrayList<>();

  CallLog(@Value("${spike.calls-log}") String file) {
    this.file = Path.of(file);
  }

  record Result(
      String capability,
      String inputId,
      boolean ok,
      long latencyMs,
      Integer inputTokens,
      Integer outputTokens,
      int httpCalls,
      String answer,
      String error) {}

  void record(Result r) {
    double cost =
        (r.inputTokens() == null ? 0 : r.inputTokens() * INPUT_PER_TOKEN)
            + (r.outputTokens() == null ? 0 : r.outputTokens() * OUTPUT_PER_TOKEN);
    String summary =
        String.format(
            Locale.ROOT,
            "[%s] %s %s %dms in=%s out=%s httpCalls=%d $%.5f%s",
            r.capability(),
            r.inputId(),
            r.ok() ? "ok" : "FAIL",
            r.latencyMs(),
            r.inputTokens(),
            r.outputTokens(),
            r.httpCalls(),
            cost,
            r.ok() ? " answer: " + r.answer() : " error: " + r.error());
    System.out.println(summary);
    lines.add(summary);
    String json =
        String.format(
            Locale.ROOT,
            "{\"runId\":%s,\"ts\":%s,\"capability\":%s,\"inputId\":%s,\"n\":1,\"ok\":%b,\"status\":%d,"
                + "\"rateLimited\":false,\"latencyMs\":%d,\"usage\":{\"inputTokens\":%s,\"outputTokens\":%s,"
                + "\"httpCalls\":%d},\"costUsd\":%.6f,\"error\":%s,\"metrics\":{\"answer\":%s}}",
            quote(runId),
            quote(Instant.now().toString()),
            quote(r.capability()),
            quote(r.inputId()),
            r.ok(),
            r.ok() ? 200 : 0,
            r.latencyMs(),
            r.inputTokens(),
            r.outputTokens(),
            r.httpCalls(),
            cost,
            quote(r.error()),
            quote(r.answer()));
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      Files.writeString(
          file,
          json + "\n",
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.err.println("could not append to " + file + ": " + e.getMessage());
    }
  }

  void printSummary() {
    System.out.println("\n--- " + runId + " ---");
    lines.forEach(System.out::println);
  }

  private static String quote(String s) {
    if (s == null) {
      return "null";
    }
    StringBuilder b = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"' -> b.append("\\\"");
        case '\\' -> b.append("\\\\");
        case '\n' -> b.append("\\n");
        case '\r' -> b.append("\\r");
        case '\t' -> b.append("\\t");
        default -> {
          if (c < 0x20) {
            b.append(String.format("\\u%04x", (int) c));
          } else {
            b.append(c);
          }
        }
      }
    }
    return b.append('"').toString();
  }
}
