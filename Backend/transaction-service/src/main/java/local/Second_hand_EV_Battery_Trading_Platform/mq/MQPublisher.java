package local.Second_hand_EV_Battery_Trading_Platform.mq;

import java.util.Map;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MQPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mq.exchange:ev.exchange}")
    private String exchange;

    // ✅ (Tùy chọn) Tự động tạo exchange nếu chưa tồn tại
    @Bean
    public DirectExchange evExchange() {
        return new DirectExchange(exchange, true, false);
    }

    // ✅ Gửi message có kiểm soát lỗi
    public void publish(String routingKey, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("📤 [MQPublisher] Sent event: {} | Payload: {}", routingKey, payload);
        } catch (Exception e) {
            log.error("❌ [MQPublisher] Lỗi khi gửi message tới MQ: {}", e.getMessage());
        }
    }
}
