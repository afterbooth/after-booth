package com.afterbooth.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.TestcontainersConfiguration;
import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.dto.RegisterItemRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// TEMPLATE: integration test against real PostgreSQL (Testcontainers). Run with the PowerShell tool.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ItemServiceConcurrencyTest {

  private static final int REQUESTS = 100;

  @Autowired ItemService itemService;

  @Test
  void onlyRemainingQuantitySucceedsUnderConcurrency() throws Exception {
    Long itemId =
        itemService
            .register(
                1L,
                new RegisterItemRequest(
                    ItemCategory.DISPLAY_RACK, 2, Instant.parse("2099-01-01T00:00:00Z")))
            .id();
    CountDownLatch start = new CountDownLatch(1);
    List<Future<ErrorCode>> results = new ArrayList<>();

    try (ExecutorService pool = Executors.newFixedThreadPool(32)) {
      for (int i = 0; i < REQUESTS; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    itemService.decreaseAvailable(itemId, 1);
                    return null;
                  } catch (BusinessException e) {
                    return e.errorCode();
                  }
                }));
      }
      start.countDown();

      // an unexpected exception surfaces from get() and fails the test
      List<ErrorCode> outcomes = new ArrayList<>();
      for (Future<ErrorCode> result : results) {
        outcomes.add(result.get(30, TimeUnit.SECONDS));
      }

      assertThat(outcomes).filteredOn(code -> code == null).hasSize(2);
      assertThat(outcomes).filteredOn(code -> code == ErrorCode.QTY_UNAVAILABLE).hasSize(98);
    }
    assertThat(itemService.get(itemId).availableQty()).isZero();
  }
}
