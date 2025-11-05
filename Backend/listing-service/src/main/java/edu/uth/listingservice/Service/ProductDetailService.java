package edu.uth.listingservice.Service;

import java.util.List;
import java.util.Collections;
import java.util.Set; 
import org.springframework.beans.factory.annotation.Autowired;
// BỎ: import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service;
// BỎ: import org.springframework.web.reactive.function.client.WebClient; 
// BỎ: import reactor.core.publisher.Mono; 

import edu.uth.listingservice.DTO.ProductDetailDTO;
import edu.uth.listingservice.DTO.UserDTO;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductImageRepository;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;
import org.springframework.cache.annotation.Cacheable; 
import org.springframework.transaction.annotation.Transactional; // Tùy chọn, nếu cần Transaction cho các Repository

@Service
public class ProductDetailService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository imageRepository;
    @Autowired private ProductSpecificationRepository specificationRepository;
    @Autowired private ProductListingRepository listingRepository;

    // ĐÃ BỎ:
    // @Autowired private WebClient.Builder webClientBuilder; 
    // @Value("${user.service.baseurl}") 

    /**
     * Lấy thông tin người bán từ Cache "users" (Redis).
     *
     * Nếu cache miss (key không tồn tại), phương thức này sẽ được gọi
     * và trả về Fallback User (Unknown Seller) vì không còn WebClient để gọi User Service.
     * Khi đó, kết quả Fallback sẽ được lưu lại vào cache 'users' trong TTL (5 phút).
     */
    @Cacheable(value = "users", key = "#userId")
    public UserDTO getSellerDetails(Long userId) {
        // Nếu code thực thi đến đây, nghĩa là cache miss.
        // Dữ liệu sẽ được lấy từ MQ và lưu vào cache bởi UserEventListener.
        // Nếu sự kiện MQ chưa đến, chúng ta trả về Fallback User để tránh lỗi.
        return createFallbackUser(userId);
    }

    @Cacheable(value = "productDetails", key = "#productId") 
    @Transactional // Giữ nguyên Transaction cho các thao tác DB cục bộ
    public ProductDetailDTO getProductDetail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        List<ProductImage> images = imageRepository.findByProduct_ProductId(productId);
        ProductSpecification spec = specificationRepository.findByProduct_ProductId(productId);
        ProductListing listing = listingRepository.findByProduct_ProductId(productId);

        UserDTO seller = null;
        if (listing != null && listing.getUserId() != null) {
            // === GỌI HÀM LẤY THÔNG TIN TỪ CACHE CỤC BỘ ===
            seller = getSellerDetails(listing.getUserId());
            // ===========================================
        }

        return new ProductDetailDTO(product, images, spec, listing, seller);
    }

    /**
     * Helper: Tạo một UserDTO mặc định khi không tìm thấy User trong Cache
     * (Hoạt động như Fallback/Placeholder)
     */
    private UserDTO createFallbackUser(Long userId) { 
        UserDTO fallback = new UserDTO();
        fallback.setId(userId.intValue()); 
        fallback.setName("Unknown Seller (ID: " + userId + ")");
        fallback.setEmail("N/A");
        fallback.setPhone("N/A");
        fallback.setAddress("N/A");
        // Bỏ Roles:
        // fallback.setRoles(Collections.emptySet()); 
        return fallback;
    }
}