package com.afterbooth.inventory.service;

import com.afterbooth.global.error.BusinessException;
import com.afterbooth.global.error.ErrorCode;
import com.afterbooth.inventory.domain.Item;
import com.afterbooth.inventory.domain.VehicleType;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class VehicleFitPolicy {
  private final Environment environment;

  public VehicleFitPolicy(Environment environment) {
    this.environment = environment;
  }

  public void requireFit(Item item, VehicleType vehicle) {
    String prefix =
        "afterbooth.vehicle-cargo."
            + vehicle.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    Integer width = environment.getProperty(prefix + ".width-cm", Integer.class);
    Integer depth = environment.getProperty(prefix + ".depth-cm", Integer.class);
    Integer height = environment.getProperty(prefix + ".height-cm", Integer.class);
    if (width == null || depth == null || height == null || width <= 0 || depth <= 0 || height <= 0)
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "차량 적재 공간 설정을 확인해 주세요.");
    int[] cargo = {width, depth, height};
    int[] dimensions = {
      item.dimensions().widthCm(), item.dimensions().depthCm(), item.dimensions().heightCm()
    };
    Arrays.sort(cargo);
    Arrays.sort(dimensions);
    if ((item.vehicleRequirement() != null
            && vehicle.ordinal() < item.vehicleRequirement().ordinal())
        || dimensions[0] > cargo[0]
        || dimensions[1] > cargo[1]
        || dimensions[2] > cargo[2]) throw new BusinessException(ErrorCode.VEHICLE_TOO_SMALL);
  }
}
