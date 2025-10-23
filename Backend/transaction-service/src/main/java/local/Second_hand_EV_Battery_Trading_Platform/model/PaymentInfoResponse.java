package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfoResponse {

    private String transactionId;
    private String status;       // SUCCESS / FAILED / PENDING
    private String method;       // VNPay / MoMo / Cash

    // === Thông tin khách hàng ===
    private String fullName;
    private String phone;
    private String email;
    private String address;

    // === Thông tin sản phẩm (lấy từ Cart) ===
    private String productName;
    private double price;
    private double totalAmount;
}
