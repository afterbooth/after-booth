package com.afterbooth.intake.pipeline;

import com.afterbooth.ai.AiGateway;
import com.afterbooth.ai.ChatRequest;
import com.afterbooth.ai.ChatResponse;
import com.afterbooth.ai.Image;
import com.afterbooth.ai.PixelBox;
import com.afterbooth.ai.SamRequest;
import com.afterbooth.ai.SamSegment;
import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.inventory.domain.Condition;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IntakePipeline {
  private final AiGateway gateway;
  private final JsonMapper json;
  private final String reasoningEffort;
  private final BigDecimal confidenceThreshold;
  private final double samMinScore;

  public IntakePipeline(
      AiGateway gateway,
      JsonMapper json,
      @Value("${afterbooth.ai.intake.reasoning-effort}") String reasoningEffort,
      @Value("${afterbooth.ai.intake.confidence-threshold}") BigDecimal confidenceThreshold,
      @Value("${afterbooth.ai.sam.min-score}") double samMinScore) {
    if (confidenceThreshold.compareTo(BigDecimal.ZERO) < 0
        || confidenceThreshold.compareTo(BigDecimal.ONE) > 0
        || !Double.isFinite(samMinScore)
        || samMinScore < 0
        || samMinScore > 1) {
      throw new IllegalArgumentException(
          "Intake confidence thresholds must be between zero and one");
    }
    this.gateway = gateway;
    this.json = json;
    this.reasoningEffort = reasoningEffort;
    this.confidenceThreshold = confidenceThreshold;
    this.samMinScore = samMinScore;
  }

  public List<EstimatedLine> estimate(IntakePhoto photo) {
    List<ChatRequest.Message> messages = new ArrayList<>();
    messages.add(
        new ChatRequest.SystemMessage(
            """
        Identify reusable booth equipment: display racks, storage bins and folding tables.
        Return one line per category, counting visible units with one normalized x/y/width/height
        box per unit (origin top-left). Ignore unrelated objects. Use UNKNOWN for equipment whose
        category is uncertain. Estimate per-unit size in centimeters (null if indeterminate),
        condition and confidence. Return only the JSON required by the response schema.
        """));
    messages.add(
        new ChatRequest.UserMessage(
            "Create an inventory draft from this photo.",
            List.of(new Image(photo.bytes(), photo.mediaType()))));
    for (int attempt = 0; attempt < 2; attempt++) {
      ChatResponse response =
          gateway.chat(
              new ChatRequest(
                  messages,
                  new ChatRequest.ResponseSchema("intake_estimate", EstimateSchema.JSON),
                  List.of(),
                  reasoningEffort));
      try {
        return lines(EstimateSchema.parse(json, response.content()));
      } catch (IllegalArgumentException | tools.jackson.core.JacksonException invalid) {
        if (attempt == 1)
          throw new IllegalArgumentException("Spark output failed validation twice", invalid);
        messages.add(
            new ChatRequest.AssistantMessage(
                response.content() == null ? "" : response.content(), List.of()));
        messages.add(
            new ChatRequest.UserMessage(
                "The answer failed schema validation. Correct types, enums, bounds and required fields. "
                    + "Use one line per category and exactly count boxes, all inside the image. Return corrected JSON only.",
                List.of()));
      }
    }
    throw new IllegalStateException("Unreachable estimate attempt");
  }

  private List<EstimatedLine> lines(JsonNode result) {
    List<EstimatedLine> lines = new ArrayList<>();
    for (JsonNode line : result.path("lines")) {
      if (line.path("count").asInt() == 0) continue;
      DraftCategory category = DraftCategory.valueOf(line.path("category").asText());
      BigDecimal confidence = new BigDecimal(line.path("confidence").asText());
      List<BoundingBox> boxes = new ArrayList<>();
      for (JsonNode box : line.path("boxes"))
        boxes.add(
            new BoundingBox(
                box.path("x").asDouble(),
                box.path("y").asDouble(),
                box.path("width").asDouble(),
                box.path("height").asDouble()));
      JsonNode size = line.path("sizeCm");
      lines.add(
          new EstimatedLine(
              category,
              line.path("count").asInt(),
              boxes,
              size.isNull() ? null : size.path("width").asInt(),
              size.isNull() ? null : size.path("depth").asInt(),
              size.isNull() ? null : size.path("height").asInt(),
              Condition.valueOf(line.path("condition").asText()),
              confidence,
              category == DraftCategory.UNKNOWN || confidence.compareTo(confidenceThreshold) < 0));
    }
    return List.copyOf(lines);
  }

  public List<EstimatedLine> segment(IntakePhoto photo, List<EstimatedLine> lines) {
    List<BoundingBox> boxes = lines.stream().flatMap(line -> line.boxes().stream()).toList();
    if (boxes.isEmpty()) return lines;
    BufferedImage image;
    try {
      image = ImageIO.read(new ByteArrayInputStream(photo.bytes()));
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot decode intake photo", e);
    }
    if (image == null) throw new IllegalArgumentException("Cannot decode intake photo");
    int width = image.getWidth();
    int height = image.getHeight();
    Map<Integer, BoundingBox> segmented = new HashMap<>();
    for (int start = 0; start < boxes.size(); start += SamRequest.Boxes.MAX_BOXES) {
      List<PixelBox> prompts =
          boxes.subList(start, Math.min(start + SamRequest.Boxes.MAX_BOXES, boxes.size())).stream()
              .map(box -> pixels(box, width, height))
              .toList();
      List<SamSegment> output =
          gateway.segment(
              new SamRequest(
                  new Image(photo.bytes(), photo.mediaType()),
                  new SamRequest.Boxes(width, height, prompts)));
      Map<Integer, Long> occurrences = new HashMap<>();
      output.forEach(segment -> occurrences.merge(segment.id(), 1L, Long::sum));
      for (SamSegment segment : output) {
        if (segment.id() < 1
            || segment.id() > prompts.size()
            || occurrences.get(segment.id()) != 1
            || !Double.isFinite(segment.score())
            || segment.score() < samMinScore
            || segment.score() > 1
            || segment.mask() == null
            || segment.mask().data() == null
            || segment.mask().data().isBlank()) continue;
        PixelBox box = segment.box();
        if (box == null
            || box.x1() < 0
            || box.y1() < 0
            || box.x2() < box.x1()
            || box.y2() < box.y1()
            || box.x2() >= width
            || box.y2() >= height) continue;
        segmented.put(
            start + segment.id() - 1,
            new BoundingBox(
                (double) box.x1() / width,
                (double) box.y1() / height,
                (double) (box.x2() - box.x1() + 1) / width,
                (double) (box.y2() - box.y1() + 1) / height));
      }
    }
    List<EstimatedLine> result = new ArrayList<>();
    int index = 0;
    for (EstimatedLine line : lines) {
      List<BoundingBox> refined = new ArrayList<>();
      boolean incomplete = false;
      for (BoundingBox original : line.boxes()) {
        BoundingBox tight = segmented.get(index++);
        incomplete |= tight == null;
        refined.add(tight == null ? original : tight);
      }
      result.add(line.segmented(refined, incomplete));
    }
    return List.copyOf(result);
  }

  private static PixelBox pixels(BoundingBox box, int width, int height) {
    int x1 = Math.min(width - 1, (int) Math.floor(box.x() * width));
    int y1 = Math.min(height - 1, (int) Math.floor(box.y() * height));
    return new PixelBox(
        x1,
        y1,
        Math.max(x1, Math.min(width - 1, (int) Math.ceil((box.x() + box.width()) * width) - 1)),
        Math.max(y1, Math.min(height - 1, (int) Math.ceil((box.y() + box.height()) * height) - 1)));
  }
}
