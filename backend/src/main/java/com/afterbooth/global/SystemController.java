package com.afterbooth.global;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/health from contract/openapi.yaml. The frontend shows the current AI mode from it
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
