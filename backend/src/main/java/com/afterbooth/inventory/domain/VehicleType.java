package com.afterbooth.inventory.domain;

public enum VehicleType {
  SEDAN(1),
  SUV(2),
  VAN(3),
  TRUCK_1T(4);

  private final int cargoRank;

  VehicleType(int cargoRank) {
    this.cargoRank = cargoRank;
  }

  public boolean canCarry(VehicleType required) {
    return required == null || cargoRank >= required.cargoRank;
  }
}
