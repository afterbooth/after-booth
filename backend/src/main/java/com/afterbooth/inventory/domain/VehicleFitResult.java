package com.afterbooth.inventory.domain;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;

public record VehicleFitResult(boolean fit, ErrorCode code, String limitingDimension) {
  static VehicleFitResult evaluate(Item item, VehicleType vehicle, Dimensions cargo) {
    String limitingDimension = item.dimensions().limitingDimension(cargo);
    if (limitingDimension != null || !vehicle.canCarry(item.vehicleRequirement())) {
      return new VehicleFitResult(false, ErrorCode.VEHICLE_TOO_SMALL, limitingDimension);
    }
    return new VehicleFitResult(true, null, null);
  }

  public void requireFit() {
    if (!fit) throw new BusinessException(code);
  }
}
