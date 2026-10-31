package com.afterbooth.intake.controller;

import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.global.error.ErrorResponse;
import com.afterbooth.intake.domain.IntakeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IntakeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class IntakeExceptionHandler {
  @ExceptionHandler(IntakeException.class)
  ResponseEntity<ErrorResponse> intake(IntakeException e) {
    return ResponseEntity.status(e.error().status())
        .body(new ErrorResponse(e.error().name(), e.getMessage()));
  }

  @ExceptionHandler(MissingRequestValueException.class)
  ResponseEntity<ErrorResponse> missing(Exception e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("INVALID_INPUT", ErrorCode.INVALID_INPUT.message()));
  }
}
