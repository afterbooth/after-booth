package com.afterbooth.request.dto;

import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import com.afterbooth.request.domain.RequestFailure;
import java.time.OffsetDateTime;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public record CreatePickupRequest(
    Long organizationId,
    ItemCategory category,
    Integer quantity,
    VehicleType vehicleType,
    OffsetDateTime pickupDeadline,
    String purpose,
    String transcript) {
  public static CreatePickupRequest parse(JsonMapper json, String body) {
    try {
      JsonNode tree = json.readTree(body);
      if (tree == null
          || !tree.isObject()
          || !tree.path("quantity").isIntegralNumber()
          || !tree.path("quantity").canConvertToInt()
          || !tree.path("organizationId").isIntegralNumber()
          || !tree.path("organizationId").canConvertToLong()) throw RequestFailure.invalid();
      for (String field : List.of("category", "vehicleType")) {
        if (!tree.path(field).isString()) throw RequestFailure.invalid();
      }
      for (String field : List.of("pickupDeadline", "purpose", "transcript")) {
        if (tree.has(field) && !tree.get(field).isString()) throw RequestFailure.invalid();
      }
      CreatePickupRequest input = json.readValue(body, CreatePickupRequest.class);
      input.validate();
      return input;
    } catch (JacksonException e) {
      throw RequestFailure.invalid();
    }
  }

  public void validate() {
    if (organizationId == null
        || category == null
        || quantity == null
        || quantity < 1
        || vehicleType == null) throw RequestFailure.invalid();
  }
}
