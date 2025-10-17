package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long cartId;
    private String paymentMethod;
    private CustomerDTO customer; // 👈 thêm dòng này
}
