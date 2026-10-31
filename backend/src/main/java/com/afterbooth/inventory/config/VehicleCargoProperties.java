package com.afterbooth.inventory.config;

import com.afterbooth.inventory.domain.Dimensions;
import com.afterbooth.inventory.domain.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("afterbooth.vehicle-cargo")
public record VehicleCargoProperties(
    @NotNull @Valid Cargo sedan,
    @NotNull @Valid Cargo suv,
    @NotNull @Valid Cargo van,
    @NotNull @Valid Cargo truck1t) {

  public Dimensions dimensionsFor(VehicleType vehicle) {
    return switch (vehicle) {
      case SEDAN -> sedan.dimensions();
      case SUV -> suv.dimensions();
      case VAN -> van.dimensions();
      case TRUCK_1T -> truck1t.dimensions();
    };
  }

  public record Cargo(@Positive int widthCm, @Positive int depthCm, @Positive int heightCm) {
    Dimensions dimensions() {
      return new Dimensions(widthCm, depthCm, heightCm);
    }
  }
}
