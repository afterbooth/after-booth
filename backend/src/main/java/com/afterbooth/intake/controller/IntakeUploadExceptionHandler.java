package com.afterbooth.intake.controller;

import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.error.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IntakeUploadExceptionHandler {
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("INVALID_INPUT", ErrorCode.INVALID_INPUT.message()));
  }
}
