package com.afterbooth.request.controller;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.request.domain.ConditionContext;
import com.afterbooth.request.dto.ConditionInterpretation;
import com.afterbooth.request.service.ConditionService;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
class ConditionController {
  private final ConditionService service;

  ConditionController(ConditionService service) {
    this.service = service;
  }

  @PostMapping(value = "/api/conditions/interpret", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ConditionInterpretation interpret(
      @RequestParam ConditionContext context,
      @RequestParam(required = false) Long eventId,
      @RequestParam(required = false) MultipartFile audio,
      @RequestParam(required = false) String text) {
    try {
      return service.interpret(
          context,
          eventId,
          audio == null ? null : audio.getBytes(),
          audio == null ? null : audio.getContentType(),
          text);
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
  }
}
