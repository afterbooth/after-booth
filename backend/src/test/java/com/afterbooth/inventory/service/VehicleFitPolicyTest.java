package com.afterbooth.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.config.VehicleCargoConfig;
import com.afterbooth.inventory.config.VehicleCargoProperties;
import com.afterbooth.inventory.domain.Condition;
import com.afterbooth.inventory.domain.Dimensions;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.ItemCategory;
import com.afterbooth.inventory.domain.VehicleType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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

  ApplicationContextRunner applicationContext() {
    return new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(VehicleCargoConfig.class, VehicleFitPolicy.class);
  }

  @Test
  void applicationDefaultsDefineCargoForEveryVehicle() {
    applicationContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              VehicleCargoProperties properties = context.getBean(VehicleCargoProperties.class);
              assertThat(properties.dimensionsFor(VehicleType.SEDAN))
                  .isEqualTo(new Dimensions(120, 80, 60));
              assertThat(properties.dimensionsFor(VehicleType.SUV))
                  .isEqualTo(new Dimensions(160, 100, 100));
              assertThat(properties.dimensionsFor(VehicleType.VAN))
                  .isEqualTo(new Dimensions(240, 140, 190));
              assertThat(properties.dimensionsFor(VehicleType.TRUCK_1T))
                  .isEqualTo(new Dimensions(300, 160, 200));
            });
  }

  @Test
  void everyVehicleAcceptsBoundaryDimensionsWithAxisRotation() {
    applicationContext()
        .run(
            context -> {
              VehicleFitPolicy policy = context.getBean(VehicleFitPolicy.class);
              VehicleCargoProperties properties = context.getBean(VehicleCargoProperties.class);
              for (VehicleType vehicle : VehicleType.values()) {
                Dimensions cargo = properties.dimensionsFor(vehicle);
                policy.requireFit(
                    item(
                        new Dimensions(cargo.heightCm(), cargo.widthCm(), cargo.depthCm()),
                        vehicle),
                    vehicle);
              }
            });
  }

  @Test
  void everyVehicleRejectsOversizedDimensions() {
    applicationContext()
        .run(
            context -> {
              VehicleFitPolicy policy = context.getBean(VehicleFitPolicy.class);
              VehicleCargoProperties properties = context.getBean(VehicleCargoProperties.class);
              for (VehicleType vehicle : VehicleType.values()) {
                Dimensions cargo = properties.dimensionsFor(vehicle);
                int maximum =
                    Math.max(cargo.widthCm(), Math.max(cargo.depthCm(), cargo.heightCm()));
                assertVehicleTooSmall(
                    () ->
                        policy.requireFit(item(new Dimensions(maximum + 1, 1, 1), null), vehicle));
                int minimum =
                    Math.min(cargo.widthCm(), Math.min(cargo.depthCm(), cargo.heightCm()));
                assertVehicleTooSmall(
                    () ->
                        policy.requireFit(
                            item(new Dimensions(minimum + 1, minimum + 1, minimum + 1), null),
                            vehicle));
              }
            });
  }

  @Test
  void minimumVehicleRequirementCannotBeBypassedBySmallItem() {
    applicationContext()
        .run(
            context -> {
              VehicleFitPolicy policy = context.getBean(VehicleFitPolicy.class);
              VehicleType[] vehicles = {
                VehicleType.SEDAN, VehicleType.SUV, VehicleType.VAN, VehicleType.TRUCK_1T
              };
              for (int required = 0; required < vehicles.length; required++) {
                for (int actual = 0; actual < vehicles.length; actual++) {
                  Item small = item(new Dimensions(1, 1, 1), vehicles[required]);
                  VehicleType vehicle = vehicles[actual];
                  if (actual < required)
                    assertVehicleTooSmall(() -> policy.requireFit(small, vehicle));
                  else policy.requireFit(small, vehicle);
                }
              }
            });
  }

  @Test
  void existingCargoOverrideKeysAreBound() {
    applicationContext()
        .withPropertyValues("afterbooth.vehicle-cargo.truck-1t.width-cm=305")
        .run(
            context ->
                assertThat(
                        context
                            .getBean(VehicleCargoProperties.class)
                            .dimensionsFor(VehicleType.TRUCK_1T))
                    .isEqualTo(new Dimensions(305, 160, 200)));
  }

  @Test
  void invalidCargoOverridesFailAtStartup() {
    for (String vehicle : new String[] {"sedan", "suv", "van", "truck-1t"}) {
      for (String axis : new String[] {"width", "depth", "height"}) {
        for (String value : new String[] {"0", "-1", "invalid", ""}) {
          applicationContext()
              .withPropertyValues(
                  "afterbooth.vehicle-cargo." + vehicle + "." + axis + "-cm=" + value)
              .run(context -> assertThat(context).hasFailed());
        }
      }
    }
  }

  void assertVehicleTooSmall(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.VEHICLE_TOO_SMALL));
  }
}
