// File: edu/uth/example/review_service/Service/ReviewService.java
package edu.uth.example.review_service.Service; 

// ... (Tất cả các import cũ giữ nguyên) ...
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.example.review_service.DTO.OrderCompletedEventDTO;
import edu.uth.example.review_service.DTO.ReviewCreatedDTO;
import edu.uth.example.review_service.DTO.ReviewDTO;
import edu.uth.example.review_service.DTO.ReviewRequestDTO;
import edu.uth.example.review_service.DTO.UserReviewStatsDTO;
import edu.uth.example.review_service.Model.Review;
import edu.uth.example.review_service.Model.ReviewableTransaction;
import edu.uth.example.review_service.Repository.ReviewRepository;
import edu.uth.example.review_service.Repository.ReviewableTransactionRepository;

@Service
public class ReviewService {

    @Autowired
    private ReviewableTransactionRepository transactionRepo;
    @Autowired
    private ReviewRepository reviewRepo;
    
    // ... (Các @Autowired và các hàm cũ giữ nguyên) ...
    @Autowired
    private RabbitTemplate rabbitTemplate; 
    @Value("${app.rabbitmq.review-events.exchange}")
    private String reviewExchange; 

    // (Các hàm createReviewableTransaction, submitReview, updateReview, ... giữ nguyên)
    // ...
    @Transactional
    public ReviewableTransaction createReviewableTransaction(OrderCompletedEventDTO dto) {
        // (Giữ nguyên)
        if (transactionRepo.findByProductId(dto.getProductId()).isPresent()) {
            throw new IllegalStateException("Giao dịch cho sản phẩm này đã được ghi nhận để đánh giá.");
        }
        Date expiresAt = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        ReviewableTransaction tx = new ReviewableTransaction(
            dto.getProductId(),
            dto.getSellerId(),
            dto.getBuyerId(),
            expiresAt,
            dto.getProductName(),
            dto.getPrice()
        );
        return transactionRepo.save(tx);
    }

    @Transactional
    public Review submitReview(Long currentUserId, ReviewRequestDTO dto) {
        // (Giữ nguyên)
        ReviewableTransaction tx = transactionRepo.findByProductId(dto.getProductId())
            .orElseThrow(() -> new RuntimeException("Giao dịch không tồn tại hoặc không được phép đánh giá."));
        Long reviewerId = currentUserId;
        Long reviewedPartyId; 
        if (currentUserId.equals(tx.getBuyerId())) {
            if (tx.isBuyerReviewed()) {
                throw new IllegalStateException("Bạn đã đánh giá giao dịch này.");
            }
            reviewedPartyId = tx.getSellerId();
            tx.setBuyerReviewed(true);
        } else if (currentUserId.equals(tx.getSellerId())) {
            if (tx.isSellerReviewed()) {
                throw new IllegalStateException("Bạn đã đánh giá giao dịch này.");
            }
            reviewedPartyId = tx.getBuyerId();
            tx.setSellerReviewed(true);
        } else {
            throw new SecurityException("Bạn không có quyền đánh giá giao dịch này.");
        }
        String reviewerName = (dto.getReviewerName() != null && !dto.getReviewerName().isEmpty()) 
                                ? dto.getReviewerName() 
                                : "Người dùng ẩn danh";
        Review review = new Review(
            tx, reviewerId, reviewedPartyId,
            dto.getRating(), dto.getComment(), reviewerName
        );
        Review savedReview = reviewRepo.save(review);
        transactionRepo.save(tx);
        try {
            ReviewCreatedDTO eventPayload = new ReviewCreatedDTO(savedReview);
            rabbitTemplate.convertAndSend(reviewExchange, "review.created", eventPayload);
        } catch (Exception e) {
             System.err.println("Lỗi khi gửi sự kiện RabbitMQ (review.created): " + e.getMessage());
        }
        return savedReview;
    }
    
    @Transactional
    public Review updateReview(Long reviewId, Long currentUserId, ReviewRequestDTO dto) {
        // (Giữ nguyên)
        Review existingReview = reviewRepo.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá này."));
        if (!existingReview.getReviewerId().equals(currentUserId)) {
            throw new SecurityException("Bạn không có quyền sửa đánh giá này.");
        }
        long daysSinceCreation = ChronoUnit.DAYS.between(existingReview.getCreatedAt().toInstant(), Instant.now());
        if (daysSinceCreation > 15) {
            throw new IllegalStateException("Đã quá 15 ngày, không thể sửa đánh giá.");
        }
        if (existingReview.getUpdatedAt() != null) {
            throw new IllegalStateException("Đánh giá này đã được sửa 1 lần và không thể sửa thêm.");
        }
        existingReview.setRating(dto.getRating());
        existingReview.setComment(dto.getComment());
        existingReview.setReviewerName(dto.getReviewerName());
        existingReview.setUpdatedAt(new Date()); 
        Review updatedReview = reviewRepo.save(existingReview);
        try {
            ReviewCreatedDTO eventPayload = new ReviewCreatedDTO(updatedReview);
            rabbitTemplate.convertAndSend(reviewExchange, "review.created", eventPayload);
        } catch (Exception e) {
             System.err.println("Lỗi khi gửi sự kiện RabbitMQ (review.updated): " + e.getMessage());
        }
        return updatedReview;
    }

    public UserReviewStatsDTO getReviewStatsForUser(Long userId) {
        // (Giữ nguyên)
         List<Object[]> results = reviewRepo.getReviewStatsForUser(userId);
        if (results == null || results.isEmpty()) {
            return new UserReviewStatsDTO(userId, 0.0, 0L);
        }
        Object[] row = results.get(0);
        Double averageRating = 0.0;
        Long totalReviews = 0L;
        if (row[0] != null) {
            averageRating = ((Number) row[0]).doubleValue();
        }
        if (row[1] != null) {
            totalReviews = ((Number) row[1]).longValue();
        }
        return new UserReviewStatsDTO(userId, averageRating, totalReviews);
    }

    public List<Review> getReviewsForUser(Long userId) {
        // (Giữ nguyên)
        return reviewRepo.findByReviewedPartyId(userId);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewableTransaction> getTasksToReview(Long userId, int page, int size) {
        // (Giữ nguyên)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "expiresAt"));
        return transactionRepo.findTasksToReviewByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReviewableTransaction> getTasksCompleted(Long userId, int page, int size) {
        // (Giữ nguyên)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expiresAt"));
        return transactionRepo.findTasksCompletedByUserId(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsAboutUser(Long userId, int page, int size) {
        // (Giữ nguyên)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepo.findByReviewedPartyId(userId, pageable);
        return reviewPage.map(ReviewDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByUser(Long userId, int page, int size) {
        // (Giữ nguyên)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepo.findByReviewerId(userId, pageable);
        return reviewPage.map(ReviewDTO::new);
    }
    
    // --- THÊM HÀM MỚI NÀY ---
    @Transactional
    public void updateReviewerName(Long userId, String newName) {
        if (userId == null || newName == null || newName.isEmpty()) {
            return;
        }
        
        // 1. Tìm tất cả review do người này viết
        List<Review> reviewsToUpdate = reviewRepo.findByReviewerId(userId);
        
        if (reviewsToUpdate.isEmpty()) {
            return; // Không có gì để cập nhật
        }

        // 2. Cập nhật tên mới
        for (Review review : reviewsToUpdate) {
            review.setReviewerName(newName);
        }

        // 3. Lưu lại
        reviewRepo.saveAll(reviewsToUpdate);
        
        // (Bạn có thể thêm logic xóa cache ở đây nếu có)
    }
    // --- KẾT THÚC HÀM MỚI ---
}