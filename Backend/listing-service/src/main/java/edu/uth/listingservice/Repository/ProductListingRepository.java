package edu.uth.listingservice.Repository;

import java.util.List;
import org.springframework.data.domain.Page; // Thêm import này

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;


@Repository
public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {
     // Tìm các tin đăng đang hoạt động, có phân trang và sắp xếp
// Phương thức này dùng cho trang quản lý của Admin
    Page<ProductListing> findByListingStatus(ListingStatus status, Pageable pageable);

// ✅ SỬA LỖI TẠI ĐÂY: Đổi kiểu trả về thành "Page"
    // Dùng cho getActiveListings để lọc theo loại sản phẩm.
    @Query("SELECT pl FROM ProductListing pl WHERE pl.listingStatus = :status AND pl.product.productType = :type")
    Page<ProductListing> findByStatusAndProductType(@Param("status") ListingStatus status, @Param("type") String type, Pageable pageable);

     // THAY ĐỔI PHƯƠNG THỨC NÀY TỪ List<> thành Page<>
    Page<ProductListing> findByUserId(Long userId, Pageable pageable);
 
     ProductListing findByProduct_ProductId(Long productId);

  // ✅ HÀM MỚI: Lấy tất cả tin đăng của user, sắp xếp theo updatedAt
    List<ProductListing> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query(value = "SELECT pl.* FROM product_listings pl JOIN products p ON pl.product_id = p.product_id " +
                   "WHERE pl.listing_status = 'ACTIVE' " +
                   "AND p.product_type = :productType " +
                   "AND p.product_id != :excludeProductId " +
                   "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<ProductListing> findRandomRelatedProducts(@Param("productType") String productType,
                                                   @Param("excludeProductId") Long excludeProductId,
                                                   @Param("limit") int limit);

     @Query("SELECT pl FROM ProductListing pl WHERE " +
           "LOWER(pl.product.productName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "CAST(pl.userId AS string) LIKE CONCAT('%', :query, '%')")
    Page<ProductListing> searchByProductNameOrUserId(@Param("query") String query, Pageable pageable);
}