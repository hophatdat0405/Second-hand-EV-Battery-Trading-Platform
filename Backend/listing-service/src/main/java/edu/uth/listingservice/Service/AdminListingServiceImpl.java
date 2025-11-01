// File: edu/uth/listingservice/Service/AdminListingServiceImpl.java
package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.DTO.ListingEventDTO; // <-- IMPORT MỚI

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // <-- IMPORT MỚI
import org.springframework.amqp.rabbit.core.RabbitTemplate; // <-- IMPORT MỚI
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Date;
import org.hibernate.Hibernate;

@Service
public class AdminListingServiceImpl implements AdminListingService {
    
    // Giữ lại WS cho Admin UI
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ProductListingRepository listingRepository;

    // === BƯỚC 1: XÓA NotificationService, THÊM RabbitTemplate ===
    // @Autowired
    // private NotificationService notificationService; // <-- ĐÃ XÓA
    
    @Autowired
    private RabbitTemplate rabbitTemplate; // <-- DỊCH VỤ HỖ TRỢ MQ

    // === BƯỚC 2: LẤY TÊN EXCHANGE/KEY TỪ CONFIG ===
    @Value("${app.rabbitmq.exchange}")
    private String listingExchange;

    @Value("${app.rabbitmq.routing-key}")
    private String notificationRoutingKey;


    @Override
    public Page<ProductListing> getListingsByStatus(ListingStatus status, Pageable pageable) {
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
        listing.setListingDate(new Date()); // Cập nhật ngày duyệt

        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        // === BƯỚC 3: XÓA CODE GỌI NOTIFICATION CŨ ===
        // (Toàn bộ khối tạo userMessage, userLink, gọi notificationService,
        // và messagingTemplate.convertAndSendToUser(...) đã bị xóa)

        // === BƯỚC 4: GỬI SỰ KIỆN QUA MQ ===
        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "APPROVED", // Trạng thái sự kiện
            null // Không có lý do
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        // === BƯỚC 5: GIỮ LẠI WS CHO ADMIN UI ===
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

        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        // === BƯỚC 3: XÓA CODE GỌI NOTIFICATION CŨ ===
        // (Đã xóa...)

        // === BƯỚC 4: GỬI SỰ KIỆN QUA MQ ===
        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "REJECTED", // Trạng thái sự kiện
            reason // Gửi kèm lý do từ chối
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        // === BƯỚC 5: GIỮ LẠI WS CHO ADMIN UI ===
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
        
        ProductListing savedListing = listingRepository.save(listing);

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        // === BƯỚC 3: XÓA CODE GỌI NOTIFICATION CŨ ===
        // (Xóa 2 khối: 1 cho listingUpdates, 1 cho notifications)
        
        // === BƯỚC 4: GỬI SỰ KIỆN QUA MQ ===
        ListingEventDTO event = new ListingEventDTO(
            savedListing.getListingId(),
            savedListing.getUserId(),
            savedListing.getProduct().getProductName(),
            "VERIFIED", // Trạng thái sự kiện
            null
        );
        rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

        // === BƯỚC 5: GIỮ LẠI WS CHO ADMIN UI ===
        AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
        messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

        return savedListing;
    }
    
    @Override
    public Page<ProductListing> searchListings(String query, Pageable pageable) {
        return listingRepository.searchByProductNameOrUserId(query, pageable);
    }
}