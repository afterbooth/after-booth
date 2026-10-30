package com.afterbooth.inventory.repository;

import com.afterbooth.inventory.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// TEMPLATE: repository. Package-private unless another package in the same domain needs it.
public interface ItemRepository extends JpaRepository<Item, Long> {

  // Conditional UPDATE: no other request can slip in between read, check and write. 0 means out of stock.
  // `qty > 0` is a second line of defense against a negative qty increasing stock
  @Modifying(clearAutomatically = true)
  @Query(
      "update Item i set i.availableQty = i.availableQty - :qty "
          + "where i.id = :id and :qty > 0 and i.availableQty >= :qty")
  int decreaseAvailable(@Param("id") Long id, @Param("qty") int qty);
}
