// File: edu/uth/listingservice/Service/PricingServiceImpl.java
package edu.uth.listingservice.Service; 

import edu.uth.listingservice.DTO.PricingRequestDTO;
import edu.uth.listingservice.DTO.PricingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // <-- IMPORT MỚI
import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient; // <-- XÓA

@Service
public class PricingServiceImpl implements PricingService {
    private final Logger logger = LoggerFactory.getLogger(PricingServiceImpl.class);
    
    // === XÓA WEBCLIENT ===
    // private final WebClient webClient; 

    // === THÊM RABBITTEMPLATE ===
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Lấy tên Exchange và Routing Key từ config
    @Value("${app.rabbitmq.ai.exchange}")
    private String aiExchange;

    @Value("${app.rabbitmq.ai.routing-key}")
    private String aiRoutingKey;

    private static final long MINIMUM_PRICE = 200_000L;
    private static final long FALLBACK_PRICE = 400_000L;

    /**
     * Xóa constructor cũ dùng WebClient.Builder
     */
    // @Autowired
    // public PricingServiceImpl(WebClient.Builder webClientBuilder) {
    //     this.webClient = webClientBuilder.build();
    // }

    @Override
    public PricingResponseDTO getSuggestedPrice(PricingRequestDTO dto) {
        try {
            logger.info("Sending price request to MQ: {}", dto);

            // === SỬ DỤNG MQ REQUEST/REPLY ===
            // Gửi DTO (Java) đến Exchange, Spring tự động chuyển thành JSON
            // Nó sẽ chờ (block) cho đến khi nhận được phản hồi
            PricingResponseDTO response = (PricingResponseDTO) rabbitTemplate.convertSendAndReceive(
                aiExchange, 
                aiRoutingKey, 
                dto
            );
            // === KẾT THÚC THAY ĐỔI ===

            if (response != null && response.getSuggestedPrice() != null) {
                if (response.getSuggestedPrice() < MINIMUM_PRICE) {
                    logger.warn("AI service trả về giá quá thấp ({}). Điều chỉnh về giá tối thiểu.", response.getSuggestedPrice());
                    return new PricingResponseDTO(MINIMUM_PRICE);
                }
                logger.info("Received price from MQ: {}", response.getSuggestedPrice());
                return response;
            } else {
                logger.warn("AI service (MQ) không trả về dữ liệu. Dùng giá tạm.");
                return new PricingResponseDTO(FALLBACK_PRICE);
            }

        } catch (Exception e) {
            // Nếu service Python không chạy, nó sẽ báo lỗi (thường là timeout)
            logger.error("LỖI khi gọi AI service (MQ): {}", e.getMessage());
            return new PricingResponseDTO(FALLBACK_PRICE); 
        }
    }
}