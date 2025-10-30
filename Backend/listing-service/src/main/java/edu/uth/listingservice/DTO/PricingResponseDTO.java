package edu.uth.listingservice.DTO;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponseDTO {
    private Long suggestedPrice;
}