package com.afterbooth.intake.pipeline;

import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class EstimateSchema {
  static final String JSON =
      """
      {"type":"object","additionalProperties":false,"required":["lines"],"properties":{
        "lines":{"type":"array","items":{"type":"object","additionalProperties":false,
          "required":["category","count","boxes","sizeCm","condition","confidence"],"properties":{
            "category":{"type":"string","enum":["DISPLAY_RACK","STORAGE_BIN","FOLDING_TABLE","UNKNOWN"]},
            "count":{"type":"integer","minimum":0,"maximum":2147483647},
            "boxes":{"type":"array","items":{"type":"object","additionalProperties":false,
              "required":["x","y","width","height"],"properties":{
                "x":{"type":"number","minimum":0,"maximum":1},
                "y":{"type":"number","minimum":0,"maximum":1},
                "width":{"type":"number","minimum":0,"maximum":1},
                "height":{"type":"number","minimum":0,"maximum":1}}}},
            "sizeCm":{"type":["object","null"],"additionalProperties":false,
              "required":["width","depth","height"],"properties":{
                "width":{"type":"integer","minimum":1,"maximum":2147483647},
                "depth":{"type":"integer","minimum":1,"maximum":2147483647},
                "height":{"type":"integer","minimum":1,"maximum":2147483647}}},
            "condition":{"type":"string","enum":["GOOD","FAIR","POOR"]},
            "confidence":{"type":"number","minimum":0,"maximum":1}
          }}}
      }}
      """;

  private EstimateSchema() {}

  static JsonNode parse(JsonMapper json, String content) {
    if (content == null) throw new IllegalArgumentException("Spark returned no JSON");
    JsonNode value =
        json.reader()
            .with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(content);
    if (value == null) throw new IllegalArgumentException("Spark returned empty JSON");
    validate(json.readTree(JSON), value);
    Set<String> categories = new HashSet<>();
    for (JsonNode line : value.path("lines")) {
      if (!categories.add(line.path("category").asText())
          || line.path("count").asInt() != line.path("boxes").size()) {
        throw new IllegalArgumentException(
            "Spark must return one line per category and one box per item");
      }
      for (JsonNode box : line.path("boxes")) {
        if (box.path("width").asDouble() <= 0
            || box.path("height").asDouble() <= 0
            || box.path("x").asDouble() + box.path("width").asDouble() > 1.000000001
            || box.path("y").asDouble() + box.path("height").asDouble() > 1.000000001) {
          throw new IllegalArgumentException("Spark box must fit inside the photo");
        }
      }
    }
    return value;
  }

  private static void validate(JsonNode schema, JsonNode value) {
    JsonNode type = schema.path("type");
    boolean matches = false;
    if (type.isArray()) {
      for (JsonNode choice : type) matches |= matches(choice.asText(), value);
    } else matches = matches(type.asText(), value);
    if (!matches) throw new IllegalArgumentException("Spark JSON has an invalid type");
    if (value.isNull()) return;
    if (schema.has("enum")) {
      boolean found = false;
      for (JsonNode choice : schema.path("enum")) found |= choice.equals(value);
      if (!found) throw new IllegalArgumentException("Spark JSON has an invalid enum");
    }
    if (value.isNumber()) {
      double number = value.asDouble();
      if (!Double.isFinite(number)
          || (schema.has("minimum") && number < schema.path("minimum").asDouble())
          || (schema.has("maximum") && number > schema.path("maximum").asDouble())) {
        throw new IllegalArgumentException("Spark JSON number is out of range");
      }
    }
    if (value.isObject()) {
      for (JsonNode required : schema.path("required")) {
        if (!value.has(required.asText()))
          throw new IllegalArgumentException("Spark JSON is missing a required field");
      }
      value
          .properties()
          .forEach(
              entry -> {
                JsonNode property = schema.path("properties").path(entry.getKey());
                if (property.isMissingNode())
                  throw new IllegalArgumentException("Spark JSON has an extra field");
                validate(property, entry.getValue());
              });
    }
    if (value.isArray()) for (JsonNode element : value) validate(schema.path("items"), element);
  }

  private static boolean matches(String type, JsonNode value) {
    return switch (type) {
      case "object" -> value.isObject();
      case "array" -> value.isArray();
      case "string" -> value.isString();
      case "integer" -> value.isIntegralNumber();
      case "number" -> value.isNumber();
      case "null" -> value.isNull();
      default -> false;
    };
  }
}
