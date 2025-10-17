package local.Second_hand_EV_Battery_Trading_Platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Cart;
import local.Second_hand_EV_Battery_Trading_Platform.repository.CartRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepo;

    // ✅ Lấy giỏ hàng theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Cart> getCart(@PathVariable Long id) {
        Cart cart = cartRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        return ResponseEntity.ok(cart);
    }
}
