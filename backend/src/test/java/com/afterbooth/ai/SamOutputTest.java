package com.afterbooth.ai;

import static com.afterbooth.ai.AiTestSupport.SPIKE_FIXTURES;
import static com.afterbooth.ai.AiTestSupport.spikeExchange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SamOutputTest {

  static Stream<String> spikeSamFixtures() throws IOException {
    try (Stream<Path> files =
        Stream.concat(
            Files.list(SPIKE_FIXTURES.resolve("sam")),
            Files.list(SPIKE_FIXTURES.resolve("sam-boxes")))) {
      return files.map(file -> SPIKE_FIXTURES.relativize(file).toString()).toList().stream();
    }
  }

  @ParameterizedTest
  @MethodSource("spikeSamFixtures")
  void parsesEveryObjectInEveryRealSpikeResponse(String fixture) {
    String text = spikeExchange(fixture).at("/response/body/output/0/content/0/text").stringValue();

    List<SamSegment> segments = SamOutput.parse(text);

    assertThat(segments).hasSize(text.split("<\\|box;", -1).length - 1);
    assertThat(segments)
        .allSatisfy(
            segment -> {
              assertThat(segment.score()).isBetween(0.0, 1.0);
              assertThat(segment.box().x2()).isGreaterThan(segment.box().x1());
              assertThat(segment.mask().data()).isNotBlank();
            });
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<0f>1<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|>",
        "<0f>1<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6|><|mask;x=0;y=0;data=1,1,!|>",
        "<0f>1<|box;x1=abc;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|><|mask;x=0;y=0;c=0.9;data=1,1,!|>",
        "<0f>1<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6;c=high|><|mask;x=0;y=0;c=0.9;data=1,1,!|>",
        "<0f>1<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|><|mask;x=0;y=one;c=0.9;data=1,1,!|>",
        "<0f>99999999999<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|><|mask;x=0;y=0;c=0.9;data=1,1,!|>",
        "<0f>1<|box;x1=1;x1=2;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|><|mask;x=0;y=0;c=0.9;data=1,1,!|>",
        "<0f>1<|box;x1=1;y1=2;x2=3;y2=4;w=5;h=6;c=0.9|><|mask;x=0;x=0;y=0;c=0.9;data=1,1,!|>"
      })
  void malformedOutputIsAlwaysASamCallFailure(String text) {
    assertThatThrownBy(() -> SamOutput.parse(text))
        .isExactlyInstanceOf(AiCallException.class)
        .satisfies(e -> assertThat(((AiCallException) e).capability()).isEqualTo(Capability.SAM));
  }
}
