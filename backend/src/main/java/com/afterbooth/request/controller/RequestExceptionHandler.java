package com.afterbooth.request.controller;

import com.afterbooth.global.error.ErrorResponse;
import com.afterbooth.request.domain.RequestFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {ConditionController.class, PickupRequestController.class})
class RequestExceptionHandler {
  @ExceptionHandler(RequestFailure.class)
  ResponseEntity<ErrorResponse> failure(RequestFailure error) {
    return ResponseEntity.status(error.status())
        .body(new ErrorResponse(error.code(), error.getMessage()));
  }

  @ExceptionHandler(MissingRequestValueException.class)
  ResponseEntity<ErrorResponse> missing(MissingRequestValueException error) {
    return failure(RequestFailure.invalid());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorResponse> oversized(MaxUploadSizeExceededException error) {
    return failure(RequestFailure.invalid());
  }
}
