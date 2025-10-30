package com.example.search_service.client;

import com.example.search_service.dto.ProductListingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ListingClient {

    // Gọi listing-service với optional type -> nếu listing-service hỗ trợ thì sẽ trả filtered list
    @GetMapping("/api/listings")
    List<ProductListingDTO> getAllListings(@RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "date") String sortBy,
                                           @RequestParam(defaultValue = "100") int limit);

    // fallback (nếu bạn vẫn muốn giữ signature cũ, có thể overload ở listing-service)
    // Note: nếu listing-service ko có overload này, Feign sẽ fail -> SearchService có fallback try/catch
}
