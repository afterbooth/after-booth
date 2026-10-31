package com.afterbooth.request.domain;

import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import com.afterbooth.request.dto.Conditions;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ConditionOutput {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Set<String> FIELDS =
      Set.of("category", "quantity", "vehicleType", "pickupDeadline", "purpose");

  private ConditionOutput() {}

  public static Conditions parse(String content) {
    if (content == null) throw new IllegalArgumentException("Missing structured output");
    JsonNode root = JSON.readTree(content);
    if (root == null || !root.isObject())
      throw new IllegalArgumentException("Expected conditions object");
    for (String name : root.propertyNames()) {
      if (!FIELDS.contains(name) || root.get(name).isNull())
        throw new IllegalArgumentException("Unexpected field");
    }
    String category = text(root, "category");
    String vehicle = text(root, "vehicleType");
    String deadline = text(root, "pickupDeadline");
    Integer quantity = null;
    if (root.has("quantity")) {
      JsonNode node = root.get("quantity");
      if (!node.isIntegralNumber() || !node.canConvertToInt() || node.intValue() < 1)
        throw new IllegalArgumentException("Invalid quantity");
      quantity = node.intValue();
    }
    return new Conditions(
        category == null ? null : ItemCategory.valueOf(category),
        quantity,
        vehicle == null ? null : VehicleType.valueOf(vehicle),
        deadline == null
            ? null
            : OffsetDateTime.parse(deadline)
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime(),
        text(root, "purpose"));
  }

  private static String text(JsonNode root, String name) {
    JsonNode value = root.get(name);
    if (value == null) return null;
    if (!value.isString() || value.stringValue().isBlank())
      throw new IllegalArgumentException("Invalid text field");
    return value.stringValue();
  }
}
