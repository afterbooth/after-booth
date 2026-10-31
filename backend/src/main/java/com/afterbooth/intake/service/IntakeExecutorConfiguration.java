package com.afterbooth.intake.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class IntakeExecutorConfiguration {
  @Bean("intakeExecutor")
  ThreadPoolTaskExecutor intakeExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(16);
    executor.setThreadNamePrefix("intake-");
    return executor;
  }
}
