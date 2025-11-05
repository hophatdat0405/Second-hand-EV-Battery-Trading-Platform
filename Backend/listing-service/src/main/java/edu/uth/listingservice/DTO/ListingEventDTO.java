// File: edu/uth/listingservice/DTO/ListingEventDTO.java
package edu.uth.listingservice.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Đây là đối tượng (thông điệp) được gửi qua RabbitMQ
 * khi có một sự kiện liên quan đến tin đăng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingEventDTO {
    
    // Dữ liệu cần thiết cho Notification Service
    private Long listingId;
    private Long userId;
    private String productName;

    // Loại sự kiện để Notification Service biết cần làm gì
    private String eventType; // Ví dụ: "APPROVED", "REJECTED", "UPDATED", "SOLD", "CREATED"
}