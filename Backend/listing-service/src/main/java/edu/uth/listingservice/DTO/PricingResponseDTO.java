package edu.uth.listingservice.DTO;

// THÊM IMPORT NÀY
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// THÊM ANNOTATION NÀY LÊN TRÊN CLASS
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponseDTO {
    private Long suggestedPrice;
}