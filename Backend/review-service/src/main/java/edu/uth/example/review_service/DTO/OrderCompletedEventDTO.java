package edu.uth.example.review_service.DTO;

import lombok.Data;

@Data
public class OrderCompletedEventDTO {
    // (Đã xóa listingId)
    private Long sellerId;
    private Long buyerId;
    private String productName; // Tên dự phòng
    private Long price;
    
    // --- KHÓA CHÍNH MỚI ---
    private Long productId;     // ID của sản phẩm (Order-Service gửi cái này)
}