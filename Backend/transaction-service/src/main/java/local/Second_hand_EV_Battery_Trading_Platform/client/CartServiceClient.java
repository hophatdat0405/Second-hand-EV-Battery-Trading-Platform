package local.Second_hand_EV_Battery_Trading_Platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import local.Second_hand_EV_Battery_Trading_Platform.model.CartDTO;

@Service
public class CartServiceClient {

    @Value("${cart.service.url}")
    private String cartServiceUrl; // ví dụ: http://localhost:8082

    private final RestTemplate restTemplate = new RestTemplate();

    public CartDTO getCartById(Long cartId) {
        String url = cartServiceUrl + "/api/carts/" + cartId;
        ResponseEntity<CartDTO> res = restTemplate.getForEntity(url, CartDTO.class);
        return res.getBody();
    }

    /** Dọn giỏ sau khi thanh toán thành công (endpoint có sẵn trong cart-service) */
    public void deleteCart(Long cartId) {
        String url = cartServiceUrl + "/api/carts/" + cartId;
        restTemplate.delete(url);
    }
}
