package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.cache.CacheManager; // <-- THÊM IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- THÊM IMPORT
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    @Autowired
    private ProductImageRepository productImageRepository;
@Autowired
    private CacheManager cacheManager;
    @Override
    public List<ProductImage> getAllImages() {
        return productImageRepository.findAll();
    }

    @Override
@Cacheable(value = "productImage", key = "#id") 
    public ProductImage getImageById(Long id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + id));
    }

    @Override
    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProduct_ProductId(productId);
    }

    @Override
    @Caching(evict = {

        @CacheEvict(value = "productImages", key = "#result.product.productId") 
    })
    public ProductImage createImage(ProductImage productImage) {
        // BỔ SUNG: Xóa cache productDetails
        if(productImage.getProduct() != null && productImage.getProduct().getProductId() != null) {
            cacheManager.getCache("productDetails").evictIfPresent(productImage.getProduct().getProductId());
        }
        return productImageRepository.save(productImage);
    }


  @Override
    @Transactional
    // BỎ HẾT ANNOTATION @Caching ở đây
    public void deleteImage(Long id) {
        // 1. Tìm ảnh TRƯỚC KHI XÓA để lấy thông tin
        ProductImage image = productImageRepository.findById(id).orElse(null);
        
        if (image == null) {
            // Không tìm thấy ảnh, không làm gì cả
            return;
        }

        Long productId = image.getProduct().getProductId();

        // 2. Xóa tất cả cache liên quan bằng CacheManager
        cacheManager.getCache("productDetails").evictIfPresent(productId);
        cacheManager.getCache("productImages").evictIfPresent(productId); // Xóa danh sách ảnh của PId
        cacheManager.getCache("productImage").evictIfPresent(id); // Xóa cache 'getById' của chính nó

        // 3. Xóa ảnh khỏi DB
        productImageRepository.deleteById(id);
    }
}
