
package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import org.springframework.data.domain.Page;

public interface AdminListingService {
    Page<ProductListing> getListingsByStatus(ListingStatus status, int page, int size);
    ProductListing approveListing(Long listingId);
    ProductListing rejectListing(Long listingId, String reason);
    ProductListing verifyListing(Long listingId);
    Page<ProductListing> searchListings(String query, int page, int size);
}