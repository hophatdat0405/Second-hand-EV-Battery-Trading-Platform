package local.Second_hand_EV_Battery_Trading_Platform.mq;

import java.util.Map;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MQPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mq.exchange:ev.exchange}")
    private String exchange;

    @Bean
    public DirectExchange evExchange() {
        return new DirectExchange(exchange, true, false);
    }

    public void publish(String routingKey, Map<String, Object> payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(exchange, routingKey, json);
            log.info("📤 [MQPublisher] Sent JSON event: {} | {}", routingKey, json);
        } catch (Exception e) {
            log.error("❌ [MQPublisher] Lỗi gửi MQ: {}", e.getMessage());
        }
    }

}

