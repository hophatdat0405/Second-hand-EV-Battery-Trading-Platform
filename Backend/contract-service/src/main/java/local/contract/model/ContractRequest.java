package local.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {
    private String transactionId; // Mã giao dịch từ transaction-service
    private String status;        // Trạng thái hợp đồng (PENDING, SIGNED, FAILED)
    private String method;        // Phương thức thanh toán (MOMO, VNPAY, ...)
    private String fullName;      // Tên khách hàng
    private String phone;         // SĐT khách hàng
    private String email;         // Email khách hàng
    private String address;       // Địa chỉ khách hàng
    private String signature;     // Chữ ký điện tử (khi ký hợp đồng)
}
