package edu.uth.listingservice.DTO;

import lombok.Data;

@Data
public class UpdateListingDTO {
    private String productName;
    private String brand;
    private Long price;
    private String description;
    private String phone;
    private String location;
    private String warrantyPolicy;
}