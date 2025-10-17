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
    List<ProductListing> findByListingStatus(ListingStatus status, Pageable pageable);

    // Tìm các tin đăng đang hoạt động THEO LOẠI SẢN PHẨM, có phân trang và sắp xếp
    @Query("SELECT pl FROM ProductListing pl WHERE pl.listingStatus = :status AND pl.product.productType = :productType")
    List<ProductListing> findByStatusAndProductType(ListingStatus status, String productType, Pageable pageable);

     // THAY ĐỔI PHƯƠNG THỨC NÀY TỪ List<> thành Page<>
    Page<ProductListing> findByUserId(Long userId, Pageable pageable);
 
     ProductListing findByProduct_ProductId(Long productId);

  

    @Query(value = "SELECT pl.* FROM product_listings pl JOIN products p ON pl.product_id = p.product_id " +
                   "WHERE pl.listing_status = 'PENDING' " +
                   "AND p.product_type = :productType " +
                   "AND p.product_id != :excludeProductId " +
                   "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<ProductListing> findRandomRelatedProducts(@Param("productType") String productType,
                                                   @Param("excludeProductId") Long excludeProductId,
                                                   @Param("limit") int limit);
}