package com.example.compare_service.client;

import com.example.compare_service.dto.ProductListingDTO;
import com.example.compare_service.dto.ProductDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ListingClient {

    @GetMapping("/api/listings")
    List<ProductListingDTO> getAllListings(@RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "date") String sortBy,
                                           @RequestParam(defaultValue = "100") int limit);

    // lấy listing theo id
    @GetMapping("/api/listings/{id}")
    ProductListingDTO getListingById(@PathVariable("id") Long id);

    // lấy chi tiết product (product-details endpoint)
    @GetMapping("/api/product-details/{productId}")
    ProductDetailDTO getProductDetail(@PathVariable("productId") Long productId);
}
