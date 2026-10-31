package com.afterbooth.intake.domain;

public enum DraftCategory {
  DISPLAY_RACK("진열대"),
  STORAGE_BIN("수납함"),
  FOLDING_TABLE("접이식 테이블"),
  UNKNOWN("미분류");
  private final String label;

  DraftCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
