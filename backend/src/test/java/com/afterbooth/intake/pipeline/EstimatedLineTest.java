package com.afterbooth.intake.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.afterbooth.global.domain.BoundingBox;
import com.afterbooth.intake.domain.DraftCategory;
import com.afterbooth.inventory.domain.Condition;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EstimatedLineTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void samFailureKeepsEveryEstimateAndRequiresReview(boolean alreadyNeedsReview) {
    EstimatedLine estimate =
        new EstimatedLine(
            DraftCategory.DISPLAY_RACK,
            1,
            List.of(new BoundingBox(0.1, 0.2, 0.3, 0.4)),
            90,
            40,
            180,
            Condition.GOOD,
            new BigDecimal("0.95"),
            alreadyNeedsReview);

    EstimatedLine result = estimate.samFailed();

    assertThat(result.needsReview()).isTrue();
    assertThat(result).usingRecursiveComparison().ignoringFields("needsReview").isEqualTo(estimate);
    assertThat(estimate.needsReview()).isEqualTo(alreadyNeedsReview);
  }
}
