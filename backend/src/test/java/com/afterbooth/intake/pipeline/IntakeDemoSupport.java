package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.ai.AiGateway;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.intake.domain.IntakeDraft;
import com.afterbooth.intake.domain.IntakeStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

final class IntakeDemoSupport {
  static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-31T03:00:00Z"), ZoneOffset.UTC);

  private IntakeDemoSupport() {}

  static ApplicationContextRunner context(String mode) {
    return new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(GatewayConfiguration.class)
        .withPropertyValues("afterbooth.ai.mode=" + mode);
  }

  static void run(AiGateway gateway) {
    IntakePhoto photo;
    try {
      photo =
          new IntakePhoto(
              Files.readAllBytes(Path.of("../spike/samples/bins-orange-stack.jpg")), "image/jpeg");
    } catch (IOException e) {
      throw new IllegalStateException(
          "Cannot read the reviewed demo photo bins-orange-stack.jpg", e);
    }
    IntakeDraft draft = IntakeDraft.start(1L, CLOCK.instant());
    IntakePipeline pipeline =
        new IntakePipeline(
            gateway, JsonMapper.builder().build(), "minimal", new BigDecimal("0.8"), 0.5);
    List<EstimatedLine> estimated = pipeline.estimate(photo);
    draft.beginSegmentation();
    List<EstimatedLine> lines = pipeline.segment(photo, estimated);
    draft.ready();
    assertThat(draft.createdAt()).isEqualTo(CLOCK.instant());
    assertThat(draft.status()).isEqualTo(IntakeStatus.READY);
    assertThat(draft.stage()).isNull();
    assertThat(lines).hasSize(1);
    EstimatedLine bins = lines.getFirst();
    assertThat(bins.category()).isEqualTo(DraftCategory.STORAGE_BIN);
    assertThat(bins.count()).isBetween(4, 5);
    assertThat(bins.boxes()).hasSize(bins.count());
    assertThat(bins.widthCm()).isPositive();
    assertThat(bins.depthCm()).isPositive();
    assertThat(bins.heightCm()).isPositive();
  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan("com.afterbooth.ai")
  static class GatewayConfiguration {}
}
