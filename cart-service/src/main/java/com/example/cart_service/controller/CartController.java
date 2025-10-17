package com.example.cart_service.controller;

import java.util.List;

import com.example.cart_service.client.ProductServiceClient;
import com.example.cart_service.dto.ProductDetailDTO;
import com.example.cart_service.model.Cart;
import com.example.cart_service.repository.CartRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RestController //biến controller thành API trả JSON thay vì view html
@RequestMapping("/api/carts") //đặt tiền tố đường dẫn
@RequiredArgsConstructor //Tự sinh constructor có tham số cho tất cả field final, giúp injection qua constructor
public class CartController {

    private final CartRepository cartRepository;
    private final ProductServiceClient productClient;  
    
    // Constructor thủ công
    // public CartController(CartRepository cartRepository, ProductServiceClient productClient) {
    //     this.cartRepository = cartRepository;
    //     this.productClient = productClient;
    // }

    //Nếu không tìm thấy key này, sẽ dùng giá trị mặc định "http://localhost:8080"
    // Dùng để biết địa chỉ (URL) của service product-service khi gọi qua Feign
    @Value("${product.service.url:http://localhost:8080}")
    private String productServiceUrl;

    // Lấy danh sách tất cả cart items
    @GetMapping
    public ResponseEntity<List<Cart>> getAll() {
        List<Cart> list = cartRepository.findAll();
        return ResponseEntity.ok(list);
    }

    // Thêm 1 item (để test postman)
    @PostMapping("/add")
    public ResponseEntity<Cart> add(@RequestBody Cart cart) {
        Cart saved = cartRepository.save(cart);
        return ResponseEntity.ok(saved);
    }

    // Thêm sản phẩm vào giỏ hàng theo productId (chỉ thêm nếu productId chưa có)
    @PostMapping("/add-by-product/{productId}")
    public ResponseEntity<?> addByProduct(@PathVariable Long productId) {
        //Nếu productId đã có trong giỏ, trả 409 Conflict (không thêm trùng)
        if (cartRepository.existsByProductId(productId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Product already in cart");
        }

        //gọi product-service (qua Feign) lấy thông tin sản phẩm
        ProductDetailDTO pd = productClient.getProductDetail(productId);
        if (pd == null || pd.getProductId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        // lấy url ảnh
        //nếu imageUrls có link đầy đủ (http/https) thì dùng 
        //nếu là đường dẫn tương đối thì ghép với productServiceUrl
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

        //build Cart entity  từ thông tin product
        Cart cart = Cart.builder()
                .productname(pd.getProductName())
                .imgurl(img)
                .price(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .total(pd.getPrice() != null ? pd.getPrice().doubleValue() : 0.0)
                .productId(pd.getProductId())
                .yearOfManufacture(pd.getYearOfManufacture())
                .brand(pd.getBrand())
                .conditionName(pd.getEffectiveConditionName())
                .mileage(pd.getMileage())
                .build();
        // Lưu vào DB và trả về bản ghi đã lưu
        Cart saved = cartRepository.save(cart);
        return ResponseEntity.ok(saved);
    }

    // Xóa theo id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (cartRepository.existsById(id)) {
            cartRepository.deleteById(id);
        }
        return ResponseEntity.noContent().build();
    }
}
