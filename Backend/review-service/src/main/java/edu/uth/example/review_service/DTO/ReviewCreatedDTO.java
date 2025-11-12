package edu.uth.example.review_service.DTO;

import edu.uth.example.review_service.Model.Review;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO này được gửi tới NotificationService để đẩy WebSocket
@Data
@NoArgsConstructor
public class ReviewCreatedDTO {
    private Long reviewedPartyId; // ID của người BỊ đánh giá
    private Review review; // Toàn bộ nội dung review

    public ReviewCreatedDTO(Review review) {
        this.review = review;
        this.reviewedPartyId = review.getReviewedPartyId();
    }
}