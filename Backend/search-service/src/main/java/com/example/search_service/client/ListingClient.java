package com.example.search_service.client;

import com.example.search_service.dto.ProductListingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ListingClient {

    @GetMapping("/api/listings")
    List<ProductListingDTO> getAllListings();

}
