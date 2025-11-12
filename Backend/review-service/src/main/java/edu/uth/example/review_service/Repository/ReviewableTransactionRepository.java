// File: edu/uth/example/review_service/Repository/ReviewableTransactionRepository.java
package edu.uth.example.review_service.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page; // <-- THÊM IMPORT
import org.springframework.data.domain.Pageable; // <-- THÊM IMPORT
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- THÊM IMPORT
import org.springframework.data.repository.query.Param; // <-- THÊM IMPORT

import edu.uth.example.review_service.Model.ReviewableTransaction;

public interface ReviewableTransactionRepository extends JpaRepository<ReviewableTransaction, Long> {
    
    Optional<ReviewableTransaction> findByProductId(Long productId);

    // --- XÓA HÀM CŨ NÀY ---
    // @EntityGraph(attributePaths = "reviews") 
    // List<ReviewableTransaction> findBySellerIdOrBuyerIdOrderByExpiresAtDesc(Long sellerId, Long buyerId);
    // --- KẾT THÚC XÓA ---

    // --- THÊM 2 HÀM PHÂN TRANG MỚI ---

    /**
     * Lấy các "vé" MÀ NGƯỜI DÙNG CHƯA ĐÁNH GIÁ (Cần làm)
     * Sắp xếp theo ngày hết hạn GẦN NHẤT
     */
    @EntityGraph(attributePaths = "reviews")
    @Query("SELECT t FROM ReviewableTransaction t WHERE " +
           "(t.sellerId = :userId AND t.isSellerReviewed = false) OR " +
           "(t.buyerId = :userId AND t.isBuyerReviewed = false) " +
           "ORDER BY t.expiresAt ASC") // Sắp xếp theo ngày hết hạn GẦN NHẤT
    Page<ReviewableTransaction> findTasksToReviewByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy các "vé" MÀ NGƯỜI DÙNG ĐÃ ĐÁNH GIÁ (Hoàn tất)
     * Sắp xếp theo ngày hết hạn MỚI NHẤT
     */
    @EntityGraph(attributePaths = "reviews")
    @Query("SELECT t FROM ReviewableTransaction t WHERE " +
           "(t.sellerId = :userId AND t.isSellerReviewed = true) OR " +
           "(t.buyerId = :userId AND t.isBuyerReviewed = true) " +
           "ORDER BY t.expiresAt DESC")
    Page<ReviewableTransaction> findTasksCompletedByUserId(@Param("userId") Long userId, Pageable pageable);
}