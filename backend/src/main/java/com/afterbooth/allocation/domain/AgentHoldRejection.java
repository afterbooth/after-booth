package com.afterbooth.allocation.domain;

import com.afterbooth.inventory.domain.ItemCategory;

public enum AgentHoldRejection {
  CATEGORY_MISMATCH;

  public static void requireCategory(ItemCategory requested, ItemCategory offered) {
    if (requested != offered) throw new AgentHoldRejectedException(CATEGORY_MISMATCH);
  }
}
