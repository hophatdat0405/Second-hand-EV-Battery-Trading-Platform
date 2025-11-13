package com.example.cart_service.listener;

import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.example.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private final CartRepository cartRepository;

    // 📨 Nhận sự kiện khi đơn hàng đã thanh toán thành công
    @RabbitListener(queues = "cart.order.paid")
    public void handleOrderPaid(Map<String, Object> data) {
        try {
            Long userId = data.get("userId") != null ? Long.parseLong(data.get("userId").toString()) : null;
            if (userId == null) {
                log.warn("⚠️ [CartService] Bỏ qua sự kiện order.paid vì thiếu userId");
                return;
            }

            // 🗑️ Xóa toàn bộ giỏ hàng của user sau khi thanh toán
            int deleted = cartRepository.deleteByUserId(userId);
            log.info("🧾 [CartService] Nhận event order.paid → Đã xóa {} sản phẩm trong giỏ hàng user #{}", deleted, userId);
        } catch (Exception e) {
            log.error("❌ [CartService] Lỗi khi xử lý order.paid: {}", e.getMessage(), e);
        }
    }
}
