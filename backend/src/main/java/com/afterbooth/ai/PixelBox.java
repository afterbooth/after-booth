package com.afterbooth.ai;

/** A box in source image pixels, corners inclusive. */
public record PixelBox(int x1, int y1, int x2, int y2) {}
