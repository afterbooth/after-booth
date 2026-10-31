package com.afterbooth.ai;

/**
 * One object SAM 3.1 found. For box prompts {@code id} is the 1-based index of the prompt box; for
 * a concept prompt it numbers the found objects from 0.
 */
public record SamSegment(int id, PixelBox box, double score, Mask mask) {

  /** The mask exactly as SAM encodes it with {@code mask_encoding: one_bit}. */
  public record Mask(int x, int y, String data) {}
}
