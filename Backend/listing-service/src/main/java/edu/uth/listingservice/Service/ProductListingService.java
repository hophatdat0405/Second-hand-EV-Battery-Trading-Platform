package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.DTO.UpdateListingDTO;
import java.util.List;

public interface ProductListingService {
    List<ProductListing> getAll();
    ProductListing getById(Long id);
    List<ProductListing> getByUserId(Long userId);
    ProductListing create(ProductListing listing);
    ProductListing update(Long id, ProductListing listing);
    void delete(Long id);
     // Thêm phương thức mới này
    List<ProductListing> getActiveListings(String type, String sortBy, int limit);
    List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit);
    ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto);
ProductListing markAsSold(Long listingId);
}
