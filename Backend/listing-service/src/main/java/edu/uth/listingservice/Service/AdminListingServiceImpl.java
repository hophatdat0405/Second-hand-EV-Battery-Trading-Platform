// File: edu/uth/listingservice/Service/AdminListingServiceImpl.java
package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.Notification;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.DTO.AdminListingUpdateDTO; // Keep DTO for admin page
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.uth.listingservice.Service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Date;
import org.hibernate.Hibernate; // Import Hibernate

@Service
public class AdminListingServiceImpl implements AdminListingService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ProductListingRepository listingRepository;
@Autowired
    private NotificationService notificationService;

    @Override
    public Page<ProductListing> getListingsByStatus(ListingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listingDate").descending());
        return listingRepository.findByListingStatus(status, pageable);
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
        listing.setAdminNotes(null);
        listing.setUpdatedAt(new Date());
        listing.setListingDate(new Date());

        ProductListing savedListing = listingRepository.save(listing);

        // Force load lazy-loaded product data
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
            // Optionally initialize images if needed later
            // Hibernate.initialize(savedListing.getProduct().getImages());
        }

        // --- Send 1: Send Notification for the bell (main.js) ---
        String userMessage = String.format("Tin đăng '%s' của bạn đã được duyệt.", savedListing.getProduct().getProductName());
        String userLink = String.format("/edit_news.html?listing_id=%d", savedListing.getListingId());
        Notification userNotification = notificationService.createNotification(savedListing.getUserId(), userMessage, userLink);

        if (userNotification != null) {
             messagingTemplate.convertAndSendToUser(
                String.valueOf(savedListing.getUserId()),
                "/topic/notifications", // Topic for the bell
                userNotification
            );
        }

        // --- Send 2: Send full ProductListing for the management page (manage-listings.js) ---
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedListing.getUserId()),
            "/topic/listingUpdates", // NEW TOPIC for management page
            savedListing // Send the full, initialized object
        );
        // ---

        // Send DTO to Admin (remains unchanged)
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
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
        listing.setAdminNotes(reason);
        listing.setUpdatedAt(new Date());
        listing.setListingDate(new Date());

        ProductListing savedListing = listingRepository.save(listing);

        // Force load lazy-loaded product data
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
            // Optionally initialize images if needed later
            // Hibernate.initialize(savedListing.getProduct().getImages());
        }

        // --- Send 1: Send Notification for the bell (main.js) ---
        String userMessage = String.format("Tin đăng '%s' của bạn đã bị từ chối.", savedListing.getProduct().getProductName());
        String userLink = String.format("/edit_news.html?listing_id=%d", savedListing.getListingId());
        Notification userNotification = notificationService.createNotification(savedListing.getUserId(), userMessage, userLink);

         if (userNotification != null) {
            messagingTemplate.convertAndSendToUser(
                String.valueOf(savedListing.getUserId()),
                "/topic/notifications", // Topic for the bell
                userNotification
            );
         }
        // ---

        // --- Send 2: Send full ProductListing for the management page (manage-listings.js) ---
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedListing.getUserId()),
            "/topic/listingUpdates", // NEW TOPIC for management page
            savedListing // Send the full, initialized object
        );
        // ---

        // Send DTO to Admin (remains unchanged)
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }

    @Override
    @Transactional
    public ProductListing verifyListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE listings can be verified.");
        }

        listing.setVerified(true);
        listing.setUpdatedAt(new Date());
        // No need to send WS message for verify action in this flow
        return listingRepository.save(listing);
    }
    
    @Override
    public Page<ProductListing> searchListings(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listingDate").descending());
        return listingRepository.searchByProductNameOrUserId(query, pageable);
    }
}