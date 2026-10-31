package com.afterbooth.ai;

import java.util.List;

/** One SAM 3.1 call on one image, prompted by a concept phrase or by boxes. */
public record SamRequest(Image image, Prompt prompt) {

  public sealed interface Prompt permits Concept, Boxes {}

  /** A noun phrase such as "folding table". */
  public record Concept(String phrase) implements Prompt {}

  /** Box prompts in source pixels of an image of the given size. */
  public record Boxes(int imageWidth, int imageHeight, List<PixelBox> boxes) implements Prompt {

    // The API rejects more than 16 objects per request (spike/REPORT.md §5)
    public static final int MAX_BOXES = 16;

    public Boxes {
      boxes = List.copyOf(boxes);
      if (boxes.isEmpty() || boxes.size() > MAX_BOXES) {
        throw new IllegalArgumentException(
            "SAM 3.1 takes 1 to "
                + MAX_BOXES
                + " box prompts per request, got "
                + boxes.size()
                + "; split the boxes into batches of "
                + MAX_BOXES);
      }
    }
  }
}
