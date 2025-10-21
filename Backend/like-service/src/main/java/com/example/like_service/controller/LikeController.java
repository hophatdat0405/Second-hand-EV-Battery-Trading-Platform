package com.example.like_service.controller;

import com.example.like_service.client.ProductServiceClient;
import com.example.like_service.dto.ProductDetailDTO;
import com.example.like_service.model.Like;
import com.example.like_service.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController //biến controller thành api trả về json thay vì view html
@RequestMapping("/api/likes") //đặt tiền tố đường dẫn
@RequiredArgsConstructor //tự sinh constructor có tham số cho tất field final, giúp injection qua constructor 
public class LikeController {
    private final LikeRepository likeRepository;
    private final ProductServiceClient productClient;

    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    @GetMapping
    public ResponseEntity<List<Like>> getAll() {
        return ResponseEntity.ok(likeRepository.findAll());
    }
    // Thêm 1 item (để test postman)
    @PostMapping("/add")
    public ResponseEntity<Like> add(@RequestBody Like like) {
        Like saved = likeRepository.save(like);
        return ResponseEntity.ok(saved);
    }
    
    @PostMapping("/add-by-product/{productId}")
    public ResponseEntity<?> addByProduct(@PathVariable Long productId) {
        if (likeRepository.existsByProductId(productId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Product already liked");
        }

        ProductDetailDTO pd = productClient.getProductDetail(productId);
        if (pd == null || pd.getProductId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        String img = "/images/product.jpg";
        if (pd.getImageUrls() != null && !pd.getImageUrls().isEmpty()) {
            String first = pd.getImageUrls().get(0);
            if (first.startsWith("http://") || first.startsWith("https://")) {
                img = first;
            } else {
                String prefix = productServiceUrl.endsWith("/") ? productServiceUrl.substring(0, productServiceUrl.length()-1) : productServiceUrl;
                img = prefix + (first.startsWith("/") ? first : ("/" + first));
            }
        }

        Like like = Like.builder()
                .productId(pd.getProductId())
                .productname(pd.getProductName())
                .imgurl(img)
                .price(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .yearOfManufacture(pd.getYearOfManufacture())
                .brand(pd.getBrand())
                .conditionName(pd.getConditionName())
                .mileage(pd.getMileage())
                .build();

        Like saved = likeRepository.save(like);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (likeRepository.existsById(id)) {
            likeRepository.deleteById(id);
        }
        return ResponseEntity.noContent().build();
    }

    // Xóa theo productId (dùng để toggle favorite từ trang chính)
    @DeleteMapping("/by-product/{productId}")
    public ResponseEntity<Void> deleteByProduct(@PathVariable Long productId) {
        Optional<Like> opt = likeRepository.findByProductId(productId);
        opt.ifPresent(like -> likeRepository.deleteById(like.getId()));
        return ResponseEntity.noContent().build();
    }
}
