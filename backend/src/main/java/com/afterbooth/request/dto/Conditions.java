package com.afterbooth.request.dto;

import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import java.time.OffsetDateTime;

public record Conditions(
    ItemCategory category,
    Integer quantity,
    VehicleType vehicleType,
    OffsetDateTime pickupDeadline,
    String purpose) {}
