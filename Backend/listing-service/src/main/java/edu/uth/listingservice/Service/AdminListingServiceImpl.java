// File: edu/uth/listingservice/Service/AdminListingServiceImpl.java
package edu.uth.listingservice.Service;
import org.springframework.cache.annotation.CacheEvict; 
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.DTO.ListingEventDTO; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Date;
import org.hibernate.Hibernate; 
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;


import java.util.List;
import java.util.ArrayList;
import edu.uth.listingservice.Model.ProductImage;


@Service
public class AdminListingServiceImpl implements AdminListingService {
    
   
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ProductListingRepository listingRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate; 
    @Value("${app.rabbitmq.exchange}")
    private String listingExchange;
    @Value("${app.rabbitmq.routing-key}")
    private String notificationRoutingKey;


    @Override
    @Cacheable(value = "adminListings", key = "#status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true) 
    public Page<ProductListing> getListingsByStatus(ListingStatus status, Pageable pageable) {
        
        Page<ProductListing> listingPage = listingRepository.findByListingStatus(status, pageable);

        // === SỬA LỖI LAZY LOADING (BƯỚC CUỐI) ===
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                // 1. "Đánh thức" collection
                Hibernate.initialize(listing.getProduct().getImages());
                
                // 2. (QUAN TRỌNG) Thay thế proxy bằng ArrayList
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        // ===================================

        return listingPage;
    }

    
    @Override
    @Cacheable(value = "adminSearchListings", key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true) 
    public Page<ProductListing> searchListings(String query, Pageable pageable) {
        
        Page<ProductListing> listingPage = listingRepository.searchByProductNameOrUserId(query, pageable);

        // === SỬA LỖI LAZY LOADING (BƯỚC CUỐI) ===
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                // 1. "Đánh thức" collection
                Hibernate.initialize(listing.getProduct().getImages());
                
                // 2. (QUAN TRỌNG) Thay thế proxy bằng ArrayList
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        // ===================================
        
        return listingPage;
    }
    
 

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "adminListings", allEntries = true), 
        @CacheEvict(value = "adminSearchListings", allEntries = true), 
        @CacheEvict(value = "productDetails", key = "#result.product.productId")
    })
    public ProductListing approveListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING listings can be approved.");
        }

        listing.setListingStatus(ListingStatus.ACTIVE);
        listing.setAdminNotes(null);
        listing.setUpdatedAt(new Date());
        listing.setListingDate(new Date()); // Cập nhật ngày duyệt

        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "APPROVED" // Trạng thái sự kiện
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "adminListings", allEntries = true), 
        @CacheEvict(value = "adminSearchListings", allEntries = true), 
        @CacheEvict(value = "productDetails", key = "#result.product.productId")
    })
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

        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "REJECTED" // Trạng thái sự kiện

        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }

    @Override
    @Transactional
@Caching(evict = {
        @CacheEvict(value = "activeListings", allEntries = true),
        @CacheEvict(value = "userListings", allEntries = true),
        @CacheEvict(value = "adminListings", allEntries = true), 
        @CacheEvict(value = "adminSearchListings", allEntries = true), 
        @CacheEvict(value = "productDetails", key = "#result.product.productId")
    })
    public ProductListing verifyListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE listings can be verified.");
        }

        listing.setVerified(true);
        listing.setUpdatedAt(new Date());
        
        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        
        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "VERIFIED" // Trạng thái sự kiện
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }
}