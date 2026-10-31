package com.afterbooth.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses SAM 3.1's special-token text, e.g. {@code
 * <0f>1<|box;x1=..;y1=..;x2=..;y2=..;w=..;h=..;c=..|><|mask;x=0;y=0;c=..;data=..|>,2<|box;...}
 * (grammar from dev.meta.ai/docs, checked against every spike/REPORT.md fixture).
 */
final class SamOutput {

  private static final Pattern OBJECT =
      Pattern.compile("(\\d+)<\\|box;([^|]*)\\|><\\|mask;([^|]*?)data=([^|]*)\\|>");
  private static final Pattern BOX_TOKEN = Pattern.compile("<\\|box;");

  private SamOutput() {}

  static List<SamSegment> parse(String text) {
    List<SamSegment> segments = new ArrayList<>();
    Matcher matcher = OBJECT.matcher(text);
    while (matcher.find()) {
      Map<String, String> box = fields(matcher.group(2));
      Map<String, String> mask = fields(matcher.group(3));
      segments.add(
          new SamSegment(
              number(matcher.group(1), "id", Integer::parseInt),
              new PixelBox(
                  integer(box, "x1"), integer(box, "y1"), integer(box, "x2"), integer(box, "y2")),
              number(field(box, "c"), "c", Double::parseDouble),
              new SamSegment.Mask(integer(mask, "x"), integer(mask, "y"), matcher.group(4))));
    }
    long objects = BOX_TOKEN.matcher(text).results().count();
    if (objects != segments.size()) {
      throw new AiCallException(
          Capability.SAM,
          "could parse only " + segments.size() + " of " + objects + " objects in the output");
    }
    return segments;
  }

  private static Map<String, String> fields(String group) {
    return Arrays.stream(group.split(";"))
        .filter(pair -> pair.contains("="))
        .map(pair -> pair.split("=", 2))
        .collect(
            Collectors.toMap(
                pair -> pair[0],
                pair -> pair[1],
                (first, second) -> {
                  throw new AiCallException(
                      Capability.SAM, "an object in the output repeats a field: " + group);
                }));
  }

  private static int integer(Map<String, String> fields, String name) {
    return number(field(fields, name), name, Integer::parseInt);
  }

  private static <T> T number(String value, String name, Function<String, T> parse) {
    try {
      return parse.apply(value);
    } catch (NumberFormatException e) {
      throw new AiCallException(
          Capability.SAM,
          null,
          "an object in the output has a non-numeric " + name + ": " + value,
          e);
    }
  }

  private static String field(Map<String, String> fields, String name) {
    String value = fields.get(name);
    if (value == null) {
      throw new AiCallException(Capability.SAM, "an object in the output has no " + name);
    }
    return value;
  }
}
