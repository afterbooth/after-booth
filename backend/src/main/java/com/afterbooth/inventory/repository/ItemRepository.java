package com.afterbooth.inventory.repository;

import com.afterbooth.inventory.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {}
