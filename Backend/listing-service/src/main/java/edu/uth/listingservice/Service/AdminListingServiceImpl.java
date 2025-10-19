// File mới: edu/uth/listingservice/Service/AdminListingServiceImpl.java
package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class AdminListingServiceImpl implements AdminListingService {

    @Autowired
    private ProductListingRepository listingRepository;

    @Override
    public Page<ProductListing> getListingsByStatus(ListingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listingDate").descending());
        return listingRepository.findByListingStatus(status, pageable); // Sẽ thêm phương thức này vào Repository
    }

    @Override
    @Transactional
    public ProductListing approveListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING listings can be approved.");
        }

        listing.setListingStatus(ListingStatus.ACTIVE);
        listing.setAdminNotes(null); // Xóa ghi chú cũ nếu có
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public ProductListing rejectListing(Long listingId, String reason) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING listings can be rejected.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty.");
        }

        listing.setListingStatus(ListingStatus.REJECTED);
        listing.setAdminNotes(reason); // Lưu lý do từ chối
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public ProductListing verifyListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        // Chỉ cho phép kiểm định các tin đang hoạt động
        if (listing.getListingStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE listings can be verified.");
        }

        listing.setVerified(true); // Gắn nhãn đã kiểm định
        listing.setUpdatedAt(new Date());
        return listingRepository.save(listing);
    }
    @Override
    public Page<ProductListing> searchListings(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listingDate").descending());
        return listingRepository.searchByProductNameOrUserId(query, pageable); // Sẽ tạo phương thức này
    }
}