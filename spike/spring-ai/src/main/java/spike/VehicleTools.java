package spike;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Stub of the server's vehicle fit tool: item 101 is a 90 x 40 x 180 cm display rack. */
class VehicleTools {

  final AtomicInteger invocations = new AtomicInteger();

  record FitResult(long itemId, String vehicleType, boolean fits, String code, String message) {}

  @Tool(
      description =
          "Check on the server whether one inventory item fits the cargo space of a vehicle type."
              + " vehicleType is one of SEDAN, SUV, VAN, TRUCK_1T.")
  FitResult checkVehicleFit(
      @ToolParam(description = "Inventory item id") long itemId,
      @ToolParam(description = "SEDAN, SUV, VAN or TRUCK_1T") String vehicleType) {
    invocations.incrementAndGet();
    boolean fits = itemId != 101 || !vehicleType.equals("SEDAN");
    return fits
        ? new FitResult(itemId, vehicleType, true, null, null)
        : new FitResult(itemId, vehicleType, false, "VEHICLE_TOO_SMALL", "높이 180cm라 승용차에 실을 수 없어요.");
  }
}
