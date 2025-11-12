package edu.uth.example.review_service.DTO;
import lombok.Data;
import lombok.AllArgsConstructor;

// DTO 3: Dữ liệu trả về khi xem thống kê Profile
@Data
@AllArgsConstructor
public class UserReviewStatsDTO {
    private Long userId;
    private Double averageRating;
    private Long totalReviews;
}