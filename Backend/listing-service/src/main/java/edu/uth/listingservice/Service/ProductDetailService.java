package edu.uth.listingservice.Service;

import java.util.List;
import java.util.Collections;
import java.util.Set; // <-- ✅ THÊM IMPORT NÀY

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient; 
import reactor.core.publisher.Mono; 

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

@Service
public class ProductDetailService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository imageRepository;
    @Autowired private ProductSpecificationRepository specificationRepository;
    @Autowired private ProductListingRepository listingRepository;

    @Autowired
    private WebClient.Builder webClientBuilder; 

    @Value("${user.service.baseurl}") 
    private String userServiceBaseUrl;

    public ProductDetailDTO getProductDetail(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        List<ProductImage> images = imageRepository.findByProduct_ProductId(productId);
        ProductSpecification spec = specificationRepository.findByProduct_ProductId(productId);
        ProductListing listing = listingRepository.findByProduct_ProductId(productId);

        
        UserDTO seller = null;
        if (listing != null && listing.getUserId() != null) {
         
            String url = userServiceBaseUrl + "/api/user/" + listing.getUserId(); 
            
            try {
                seller = webClientBuilder.build() 
                    .get() 
                    .uri(url) 
                    .retrieve() 
                    .bodyToMono(UserDTO.class) // Bây giờ DTO đã khớp
                    .onErrorResume(e -> { 
                        System.err.println("Error fetching user " + listing.getUserId() + ": " + e.getMessage());
                        // Lỗi deserialization sẽ xảy ra ở đây nếu DTO không khớp
                        return Mono.just(createFallbackUser(listing.getUserId())); 
                    })
                    .block(); 
                    
            } catch (Exception e) {
                System.err.println("Critical error fetching user " + listing.getUserId() + ": " + e.getMessage());
                seller = createFallbackUser(listing.getUserId());
            }
        }

        return new ProductDetailDTO(product, images, spec, listing, seller);
    }

    /**
     * Helper: Tạo một UserDTO mặc định khi không gọi được User Service
     */
    // SỬA TỪ Long thành Integer
    private UserDTO createFallbackUser(Long userId) { 
        UserDTO fallback = new UserDTO();
        fallback.setId(userId.intValue()); // <-- ✅ SỬA: .intValue()
        fallback.setName("Unknown Seller");
        fallback.setEmail("N/A");
        fallback.setPhone("N/A");
        fallback.setAddress("N/A");
        fallback.setRoles(Collections.emptySet()); // <-- ✅ SỬA: emptySet()
        return fallback;
    }
}