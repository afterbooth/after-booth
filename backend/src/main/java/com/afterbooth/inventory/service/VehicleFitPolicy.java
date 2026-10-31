package com.afterbooth.inventory.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.config.VehicleCargoProperties;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.VehicleType;
import org.springframework.stereotype.Component;

@Component
public class VehicleFitPolicy {
  private final VehicleCargoProperties cargo;

  public VehicleFitPolicy(VehicleCargoProperties cargo) {
    this.cargo = cargo;
  }

  public void requireFit(Item item, VehicleType vehicle) {
    if (!vehicle.canCarry(item.vehicleRequirement())
        || !item.dimensions().fitsIn(cargo.dimensionsFor(vehicle))) {
      throw new BusinessException(ErrorCode.VEHICLE_TOO_SMALL);
    }
  }
}
