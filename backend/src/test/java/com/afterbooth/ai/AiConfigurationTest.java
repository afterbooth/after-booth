package com.afterbooth.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Startup with the real application.properties: replay needs nothing, bad settings fail fast. */
class AiConfigurationTest {

  static ApplicationContextRunner applicationContext() {
    return new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(AiConfig.class, MetaExchange.class, MetaAiGateway.class);
  }

  @Test
  void replayStartsWithoutAKey() {
    applicationContext()
        .withPropertyValues("afterbooth.ai.mode=replay", "afterbooth.ai.api-key=")
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasSingleBean(AiGateway.class);
              AiProperties properties = context.getBean(AiProperties.class);
              assertThat(properties.mode()).isEqualTo(AiMode.REPLAY);
              assertThat(properties.paths().of(Capability.SAM)).isEqualTo("/v1/responses");
            });
  }

  @Test
  void anUnknownModeFailsAtStartup() {
    applicationContext()
        .withPropertyValues("afterbooth.ai.mode=replya")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .message()
                    .containsIgnoringCase("replya"));
  }

  @Test
  void liveWithoutAKeyFailsAtStartup() {
    applicationContext()
        .withPropertyValues("afterbooth.ai.mode=live", "afterbooth.ai.api-key=")
        .run(
            context ->
                assertThat(context).getFailure().rootCause().hasMessageContaining("META_API_KEY"));
  }

  @Test
  void recordWithoutAKeyFailsAtStartup() {
    applicationContext()
        .withPropertyValues("afterbooth.ai.mode=record", "afterbooth.ai.api-key=")
        .run(
            context ->
                assertThat(context).getFailure().rootCause().hasMessageContaining("META_API_KEY"));
  }

  @Test
  void aBaseUrlEndingInV1FailsAtStartup() {
    applicationContext()
        .withPropertyValues("afterbooth.ai.base-url=https://api.meta.ai/v1")
        .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("/v1"));
  }
}
