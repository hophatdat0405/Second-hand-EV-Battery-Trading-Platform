// File: src/main/java/edu/uth/listingservice/dto/events/ListingNeedsUserDataDTO.java
package edu.uth.listingservice.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingNeedsUserDataDTO {
    // DTO này Listing-Service dùng để GỬI ĐI
    private Long listingId;
    private Long userId;
}