package local.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi sau khi ký hợp đồng thành công hoặc thất bại.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {

    // 🟢 Thông điệp phản hồi cho người dùng
    private String message;

    // 📄 Link tới file hợp đồng PDF (được sinh tự động)
    private String pdfUrl;

    // 🔁 Mã giao dịch liên kết (transactionId)
    private String transactionId;

    // 🕒 Thời gian tạo hoặc ký hợp đồng (nếu muốn hiển thị thêm)
    private String signedAt;
}
