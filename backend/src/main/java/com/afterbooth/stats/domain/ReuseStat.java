package com.afterbooth.stats.domain;

import com.afterbooth.inventory.domain.ItemCategory;

public record ReuseStat(ItemCategory category, Long eventId, String eventName, long quantity) {}
