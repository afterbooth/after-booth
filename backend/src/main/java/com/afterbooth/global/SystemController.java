package com.afterbooth.global;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// contract/openapi.yaml의 GET /api/health. 프론트가 현재 AI 모드를 표시하는 데 쓴다
@RestController
class SystemController {

  private final String aiMode;

  SystemController(@Value("${afterbooth.ai.mode}") String aiMode) {
    this.aiMode = aiMode;
  }

  @GetMapping("/api/health")
  Map<String, String> health() {
    return Map.of("status", "UP", "aiMode", aiMode);
  }
}
