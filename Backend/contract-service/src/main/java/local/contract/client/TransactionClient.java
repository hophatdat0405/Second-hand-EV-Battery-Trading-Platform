package local.contract.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import local.contract.model.ContractRequest;

/**
 * 🔗 Feign client kết nối tới transaction-service
 */
@FeignClient(
    name = "transaction-service",
    url = "${transaction.service.url:http://localhost:8083}" // URL của transaction-service
)
public interface TransactionClient {

    /**
     * Gọi endpoint để lấy thông tin giao dịch theo transactionId.
     * transaction-service có endpoint: /api/payments/info/{transactionId}
     */
    @GetMapping("/api/payments/info/{transactionId}")
    ContractRequest getTransactionInfo(@PathVariable("transactionId") String transactionId);
}
