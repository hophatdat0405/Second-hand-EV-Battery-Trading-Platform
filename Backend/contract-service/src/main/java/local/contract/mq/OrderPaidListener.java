package local.contract.mq;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import local.contract.model.ContractRequest;
import local.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private final ContractService contractService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${mq.queue.order-paid}")
    public void handleOrderPaidEvent(String messageJson) {
        try {
            log.info("📥 [MQ] Nhận JSON message: {}", messageJson);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> message = mapper.readValue(messageJson, Map.class);

            String transactionId = (String) message.get("transactionId");
            String method = (String) message.get("method");

            ContractRequest req = new ContractRequest();
            req.setTransactionId(transactionId);
            req.setMethod(method);

            contractService.createContract(req);
            log.info("✅ [Contract] Đã tạo hợp đồng tự động cho transactionId={}", transactionId);

        } catch (Exception e) {
            log.error("❌ [MQ] Lỗi khi xử lý message: {}", e.getMessage(), e);
        }
    }
}
