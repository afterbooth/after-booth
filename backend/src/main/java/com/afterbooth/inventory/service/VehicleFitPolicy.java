package com.afterbooth.inventory.service;

import com.afterbooth.inventory.config.VehicleCargoProperties;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.VehicleFitResult;
import com.afterbooth.inventory.domain.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class VehicleFitPolicy {
  private final VehicleCargoProperties cargo;

  public VehicleFitPolicy(VehicleCargoProperties cargo) {
    this.cargo = cargo;
  }

  public VehicleFitResult checkFit(Item item, VehicleType vehicle) {
    return item.vehicleFit(vehicle, cargo.dimensionsFor(vehicle));
  }

  public void requireFit(Item item, VehicleType vehicle) {
    checkFit(item, vehicle).requireFit();
  }
}
