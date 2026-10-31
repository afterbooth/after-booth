package com.afterbooth.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.Condition;
import com.afterbooth.inventory.domain.Dimensions;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class VehicleFitPolicyTest {
  Item item(Dimensions dimensions, VehicleType required) {
    return Item.register(
        1L,
        1L,
        null,
        ItemCategory.STORAGE_BIN,
        dimensions,
        Condition.GOOD,
        1,
        Instant.parse("2099-01-01T00:00:00Z"),
        required,
        null);
  }

  VehicleFitPolicy policy() {
    MockEnvironment environment = new MockEnvironment();
    for (VehicleType vehicle : VehicleType.values()) {
      String prefix =
          "afterbooth.vehicle-cargo."
              + vehicle.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
      environment
          .withProperty(prefix + ".width-cm", "100")
          .withProperty(prefix + ".depth-cm", "80")
          .withProperty(prefix + ".height-cm", "60");
    }
    return new VehicleFitPolicy(environment);
  }

  @Test
  void missingCargoConfigurationFailsClosed() {
    assertThatThrownBy(
            () ->
                new VehicleFitPolicy(new MockEnvironment())
                    .requireFit(item(new Dimensions(1, 1, 1), null), VehicleType.SEDAN))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
  }

  @Test
  void everyVehicleAcceptsBoundaryDimensionsWithAxisRotation() {
    for (VehicleType vehicle : VehicleType.values())
      policy().requireFit(item(new Dimensions(60, 100, 80), vehicle), vehicle);
  }

  @Test
  void everyVehicleRejectsOversizedDimensions() {
    for (VehicleType vehicle : VehicleType.values())
      assertThatThrownBy(() -> policy().requireFit(item(new Dimensions(101, 1, 1), null), vehicle))
          .isInstanceOfSatisfying(
              BusinessException.class,
              error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.VEHICLE_TOO_SMALL));
  }

  @Test
  void minimumVehicleRequirementCannotBeBypassedBySmallItem() {
    assertThatThrownBy(
            () ->
                policy()
                    .requireFit(item(new Dimensions(1, 1, 1), VehicleType.VAN), VehicleType.SUV))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.VEHICLE_TOO_SMALL));
    policy().requireFit(item(new Dimensions(1, 1, 1), VehicleType.VAN), VehicleType.TRUCK_1T);
  }
}
