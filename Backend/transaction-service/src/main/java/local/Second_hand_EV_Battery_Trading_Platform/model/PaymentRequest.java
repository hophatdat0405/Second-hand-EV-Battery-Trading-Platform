package local.Second_hand_EV_Battery_Trading_Platform.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private List<Long> cartIds;     // ✅ Đổi từ String sang List<Long>
    private Double totalAmount;     // Tổng tiền tất cả sản phẩm
    private String paymentMethod;   // "vnpay" hoặc "momo"
    private CustomerDTO customer;   // Thông tin khách hàng
}
