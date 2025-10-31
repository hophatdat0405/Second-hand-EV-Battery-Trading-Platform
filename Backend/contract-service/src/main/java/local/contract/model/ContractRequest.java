package local.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {
    private String transactionId;
    private String status;
    private String method;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String signature; // chữ ký người dùng khi ký hợp đồng
}
