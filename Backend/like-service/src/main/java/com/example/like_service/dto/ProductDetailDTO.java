package com.example.like_service.dto;

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
}
