package edu.uth.example.review_service.DTO;
import lombok.Data;

@Data
public class ReviewRequestDTO {
    // --- THAY ĐỔI CHÍNH ---
    private Long productId; // <-- Thay thế cho listingId
    
    private int rating;     
    private String comment;
    private String reviewerName; 
}