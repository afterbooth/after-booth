package com.afterbooth.inventory.domain;

public enum ItemCategory {
  DISPLAY_RACK("진열대"),
  STORAGE_BIN("수납함"),
  FOLDING_TABLE("접이식 테이블");
  private final String label;

  ItemCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
