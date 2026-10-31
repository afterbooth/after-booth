package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afterbooth.ai.AiGateway;
import com.afterbooth.ai.ChatRequest;
import com.afterbooth.ai.ChatResponse;
import com.afterbooth.ai.PixelBox;
import com.afterbooth.ai.SamRequest;
import com.afterbooth.ai.SamSegment;
import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.inventory.domain.Condition;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class IntakePipelineTest {
  private final AiGateway gateway = mock(AiGateway.class);
  private final IntakePipeline pipeline =
      new IntakePipeline(
          gateway, JsonMapper.builder().build(), "minimal", new BigDecimal("0.8"), 0.5);
  private static final String VALID =
      """
      {"lines":[{"category":"STORAGE_BIN","count":1,"boxes":[{"x":0.1,"y":0.2,"width":0.5,"height":0.4}],
      "sizeCm":{"width":60,"depth":40,"height":35},"condition":"FAIR","confidence":0.9}]}
      """;

  @Test
  void estimatesStructuredCountsAndReplacesBoxesWithMatchingSamMasks() throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID));
    when(gateway.segment(any())).thenReturn(List.of(segment(1, 0.5)));
    List<EstimatedLine> lines = pipeline.segment(photo(), pipeline.estimate(photo()));
    assertThat(lines).hasSize(1);
    assertThat(lines.getFirst().count()).isEqualTo(1);
    assertThat(lines.getFirst().widthCm()).isEqualTo(60);
    assertThat(lines.getFirst().needsReview()).isFalse();
    assertThat(lines.getFirst().boxes()).containsExactly(new BoundingBox(0.2, 0.2, 0.3, 0.4));
    ArgumentCaptor<ChatRequest> request = ArgumentCaptor.forClass(ChatRequest.class);
    verify(gateway).chat(request.capture());
    assertThat(request.getValue().reasoningEffort()).isEqualTo("minimal");
    assertThat(request.getValue().responseSchema().schemaJson()).contains("additionalProperties");
  }

  @Test
  void reasksOnceForSchemaFailureThenSucceeds() throws Exception {
    when(gateway.chat(any()))
        .thenReturn(response(VALID.replace("\"count\":1", "\"count\":1.5")), response(VALID));
    assertThat(pipeline.estimate(photo())).hasSize(1);
    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(gateway, times(2)).chat(requests.capture());
    assertThat(requests.getAllValues().getLast().messages()).hasSize(4);
  }

  @Test
  void rejectsSecondInvalidAnswerWithoutAThirdCall() throws Exception {
    when(gateway.chat(any())).thenReturn(response("{}"));
    assertThatThrownBy(() -> pipeline.estimate(photo()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("twice");
    verify(gateway, times(2)).chat(any());
    verify(gateway, never()).segment(any());
  }

  @Test
  void rejectsExtraPropertiesAndCountsWithoutMatchingBoxes() throws Exception {
    when(gateway.chat(any()))
        .thenReturn(
            response(VALID.replace("\"count\":1", "\"count\":2")),
            response(VALID.replace("\"confidence\":0.9", "\"confidence\":0.9,\"invented\":true")));
    assertThatThrownBy(() -> pipeline.estimate(photo()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gateway, times(2)).chat(any());
  }

  @Test
  void unknownAndLowConfidenceNeedReview() throws Exception {
    when(gateway.chat(any()))
        .thenReturn(
            response(VALID.replace("STORAGE_BIN", "UNKNOWN")),
            response(VALID.replace("0.9", "0.79")));
    assertThat(pipeline.estimate(photo()).getFirst().needsReview()).isTrue();
    assertThat(pipeline.estimate(photo()).getFirst().needsReview()).isTrue();
  }

  @Test
  void emptyInventorySkipsSam() throws Exception {
    when(gateway.chat(any())).thenReturn(response("{\"lines\":[]}"));
    assertThat(pipeline.segment(photo(), pipeline.estimate(photo()))).isEmpty();
    verify(gateway, never()).segment(any());
  }

  @Test
  void zeroCountCategoryBecomesAnEmptyInventory() throws Exception {
    when(gateway.chat(any()))
        .thenReturn(
            response(
                VALID
                    .replace("\"count\":1", "\"count\":0")
                    .replace("[{\"x\":0.1,\"y\":0.2,\"width\":0.5,\"height\":0.4}]", "[]")));
    assertThat(pipeline.segment(photo(), pipeline.estimate(photo()))).isEmpty();
    verify(gateway, never()).segment(any());
  }

  @Test
  void rejectsTrailingJsonDocuments() throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID + "{}"));
    assertThatThrownBy(() -> pipeline.estimate(photo()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gateway, times(2)).chat(any());
  }

  @ParameterizedTest
  @CsvSource({"STORAGE_BIN,OTHER", "0.9,-0.1", "0.9,1.1", "0.5,1.1", "60,0"})
  void rejectsInvalidEnumsAndNumericBounds(String original, String invalid) throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID.replace(original, invalid)));
    assertThatThrownBy(() -> pipeline.estimate(photo()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gateway, times(2)).chat(any());
  }

  @Test
  void malformedAndToolOnlyAnswersReceiveOnlyOneReask() throws Exception {
    when(gateway.chat(any())).thenReturn(response("not JSON"), response(null));
    assertThatThrownBy(() -> pipeline.estimate(photo()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(gateway, times(2)).chat(any());
  }

  @Test
  void scoreOutsideUnitIntervalCannotSegmentAnItem() throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID));
    List<EstimatedLine> original = pipeline.estimate(photo());
    when(gateway.segment(any()))
        .thenReturn(List.of(segment(1, Double.NaN)), List.of(segment(1, 1.1)));
    assertThat(pipeline.segment(photo(), original).getFirst().needsReview()).isTrue();
    assertThat(pipeline.segment(photo(), original).getFirst().needsReview()).isTrue();
  }

  @Test
  void lowScoreAndMissingMasksKeepSparkBoxesAndNeedReview() throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID));
    List<EstimatedLine> originals = pipeline.estimate(photo());
    when(gateway.segment(any()))
        .thenReturn(
            List.of(segment(1, 0.49)),
            List.of(new SamSegment(1, new PixelBox(20, 20, 49, 59), 0.9, null)),
            List.of());
    for (int i = 0; i < 3; i++) {
      EstimatedLine result = pipeline.segment(photo(), originals).getFirst();
      assertThat(result.needsReview()).isTrue();
      assertThat(result.boxes()).isEqualTo(originals.getFirst().boxes());
    }
  }

  @Test
  void duplicateIdsAndInvalidBoxesCannotReplaceSparkBoxes() throws Exception {
    when(gateway.chat(any())).thenReturn(response(VALID));
    List<EstimatedLine> originals = pipeline.estimate(photo());
    when(gateway.segment(any()))
        .thenReturn(
            List.of(segment(1, 0.9), segment(1, 0.9)),
            List.of(
                new SamSegment(
                    1, new PixelBox(20, 20, 200, 59), 0.9, new SamSegment.Mask(0, 0, "abc"))));
    for (int i = 0; i < 2; i++)
      assertThat(pipeline.segment(photo(), originals).getFirst().needsReview()).isTrue();
  }

  @Test
  void batchesSeventeenBoxesAndMapsIdsWithinEachBatch() throws Exception {
    EstimatedLine line =
        new EstimatedLine(
            DraftCategory.STORAGE_BIN,
            17,
            IntStream.range(0, 17).mapToObj(i -> new BoundingBox(0, 0, 1, 1)).toList(),
            null,
            null,
            null,
            Condition.GOOD,
            new BigDecimal("0.9"),
            false);
    when(gateway.segment(any()))
        .thenAnswer(
            invocation -> {
              SamRequest request = invocation.getArgument(0);
              return IntStream.rangeClosed(1, ((SamRequest.Boxes) request.prompt()).boxes().size())
                  .mapToObj(id -> segment(id, 0.9))
                  .toList();
            });
    EstimatedLine result = pipeline.segment(photo(), List.of(line)).getFirst();
    assertThat(result.boxes()).hasSize(17);
    assertThat(result.needsReview()).isFalse();
    ArgumentCaptor<SamRequest> requests = ArgumentCaptor.forClass(SamRequest.class);
    verify(gateway, times(2)).segment(requests.capture());
    assertThat(((SamRequest.Boxes) requests.getAllValues().getFirst().prompt()).boxes())
        .hasSize(16);
    assertThat(((SamRequest.Boxes) requests.getAllValues().getLast().prompt()).boxes()).hasSize(1);
  }

  private static ChatResponse response(String content) {
    return new ChatResponse(content, List.of(), "stop", new ChatResponse.Usage(0, 0, 0, 0));
  }

  private static SamSegment segment(int id, double score) {
    return new SamSegment(
        id,
        new PixelBox(20, 20, 49, 59),
        score,
        new SamSegment.Mask(20, 20, "synthetic-test-mask"));
  }

  private static IntakePhoto photo() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), "png", bytes);
    return new IntakePhoto(bytes.toByteArray(), "image/png");
  }
}
