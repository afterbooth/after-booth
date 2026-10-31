package com.afterbooth.allocation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HoldItemRequest(
    @NotNull @Positive Long requestId, @NotNull @Positive Long itemId, @Positive int quantity) {}
