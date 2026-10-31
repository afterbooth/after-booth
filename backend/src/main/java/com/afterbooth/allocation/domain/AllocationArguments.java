package com.afterbooth.allocation.domain;

import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public record AllocationArguments(Long itemId, int quantity, Map<String, Object> values) {
  public static AllocationArguments parse(JsonMapper json, String name, String raw) {
    Set<String> allowed =
        switch (name) {
          case "searchCandidates" -> Set.of();
          case "checkVehicleFit" -> Set.of("itemId");
          case "holdQuantity" -> Set.of("itemId", "quantity");
          default -> throw new IllegalArgumentException("Unknown allocation tool");
        };
    if (raw == null || raw.length() > 4096) invalid();
    JsonNode node = json.readTree(raw);
    if (node == null || !node.isObject() || node.size() != allowed.size()) invalid();
    for (String field : node.propertyNames()) if (!allowed.contains(field)) invalid();
    if (allowed.isEmpty()) return new AllocationArguments(null, 0, Map.of());
    JsonNode id = node.get("itemId");
    if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0)
      invalid();
    if (name.equals("checkVehicleFit"))
      return new AllocationArguments(id.longValue(), 0, Map.of("itemId", id.longValue()));
    JsonNode qty = node.get("quantity");
    if (qty == null || !qty.isIntegralNumber() || !qty.canConvertToInt() || qty.intValue() <= 0)
      invalid();
    return new AllocationArguments(
        id.longValue(),
        qty.intValue(),
        Map.of("itemId", id.longValue(), "quantity", qty.intValue()));
  }

  private static void invalid() {
    throw new IllegalArgumentException("Invalid allocation tool arguments");
  }
}
