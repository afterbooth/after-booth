package com.afterbooth.request.dto;

import com.afterbooth.request.domain.ConditionContext;
import java.util.List;

public record ConditionInterpretation(
    ConditionContext context,
    String transcript,
    Conditions conditions,
    List<String> missingFields) {}
