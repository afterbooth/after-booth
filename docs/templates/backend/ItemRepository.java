package com.afterbooth.inventory.repository;

import com.afterbooth.inventory.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// TEMPLATE: repository. Package-private unless another package in the same domain needs it.
public interface ItemRepository extends JpaRepository<Item, Long> {

  // 조건부 UPDATE: 읽고-검사하고-쓰기 사이에 다른 요청이 끼어들 수 없다. 반환값 0이면 재고 부족.
  // qty > 0 조건은 음수로 재고를 늘리는 호출을 DB에서도 막는 이중 방어다
  @Modifying(clearAutomatically = true)
  @Query(
      "update Item i set i.availableQty = i.availableQty - :qty "
          + "where i.id = :id and :qty > 0 and i.availableQty >= :qty")
  int decreaseAvailable(@Param("id") Long id, @Param("qty") int qty);
}
