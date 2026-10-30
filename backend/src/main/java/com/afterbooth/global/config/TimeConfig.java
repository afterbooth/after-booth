package com.afterbooth.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfig {

  // 철거 마감·HOLD 만료를 테스트에서 고정 시각으로 검증하려고 Clock을 주입받는다
  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }
}
