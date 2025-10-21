package com.example.cart_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Long productId;
    private String productName;
    private Long price; 
    private List<String> imageUrls;

    private Integer yearOfManufacture;
    private String brand;
    private String conditionName; 
    private Long mileage;

    // nếu product-service trả nested: "condition": { "conditionName": "Mới 99% (Lướt)" }
    private ConditionDTO condition;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionDTO {
        private String conditionName;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public String getEffectiveConditionName() {
        String nested = (condition == null) ? null : condition.getConditionName();
        if (hasText(nested)) return nested;
        return hasText(conditionName) ? conditionName : null;
    }
}
