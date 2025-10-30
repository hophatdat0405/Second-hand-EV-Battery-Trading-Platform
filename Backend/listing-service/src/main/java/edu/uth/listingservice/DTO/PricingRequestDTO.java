package edu.uth.listingservice.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PricingRequestDTO {

    // --- Các trường cũ (Đã có) ---
    private String productType;
    private String brand;
    private Integer yearOfManufacture;
    private Integer conditionId;
    private String warrantyPolicy;
    private Integer mileage;
    private Integer rangePerCharge;
    private String batteryCapacity;
    private String batteryType;
    private Integer chargeCycles;
    private String batteryLifespan;

    // --- ✅ 3 TRƯỜNG BỊ THIẾU (BỔ SUNG) ---
    private Integer maxSpeed;
    private String color;
    private String chargeTime; // (Nên thêm cả trường này, vì JS cũng gửi nó)
    private String compatibleVehicle;

}